/*
 * *******************************************************************************
 *  * Copyright (c) 2023 Datasance Teknoloji A.S.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v. 2.0 which is available at
 *  * http://www.eclipse.org/legal/epl-2.0
 *  *
 *  * SPDX-License-Identifier: EPL-2.0
 *  *******************************************************************************
 *
 */
package org.eclipse.iofog.volume_mount;

import jakarta.json.*;
import org.eclipse.iofog.exception.AgentSystemException;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.eclipse.iofog.status_reporter.StatusReporter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.LinkOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.FileSystems;
import java.nio.file.attribute.GroupPrincipal;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.commons.lang3.SystemUtils;

/**
 * Manages volume mounts for microservices
 * Handles creation, updates, and deletion of volume mounts
 * Manages the index file and directory structure
 */
public class VolumeMountManager {
    private static final String MODULE_NAME = "VolumeMountManager";
    private static final String VOLUMES_DIR = "volumes";
    private static final String INDEX_FILE = "index.json";
    private static final String SECRETS_DIR = "secrets";
    private static final String CONFIGMAPS_DIR = "configMaps";
    private static final String MICROSERVICES_DIR = "microservices";
    private static final int MAX_VERSION_HISTORY = 1; // Keep only current version for simplicity
    private static final String DATA_SYMLINK = "..data";
    
    private static VolumeMountManager instance;
    private final String baseDirectory;
    private JsonObject indexData;
    private final Object indexLock = new Object();
    private final Map<String, VolumeMountType> typeCache = new ConcurrentHashMap<>();
    
    private VolumeMountManager() {
        this.baseDirectory = Configuration.getDiskDirectory() + VOLUMES_DIR + "/";
        init();
    }
    
    public static VolumeMountManager getInstance() {
        if (instance == null) {
            synchronized (VolumeMountManager.class) {
                if (instance == null) {
                    instance = new VolumeMountManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initializes the volume mount manager
     * Creates necessary directories and loads index file
     */
    private void init() {
        try {
            LoggingService.logInfo(MODULE_NAME, "Initializing volume mount manager");
            // Create volumes directory if it doesn't exist
            Path volumesPath = Paths.get(baseDirectory);
            if (!Files.exists(volumesPath)) {
                LoggingService.logDebug(MODULE_NAME, "Creating volumes directory at: " + baseDirectory);
                Files.createDirectories(volumesPath);
            }
            setDirectoryPermissions(volumesPath);

            // Load or create index file
            loadIndex();
            // Rebuild type cache after loading index
            rebuildTypeCache();
            LoggingService.logInfo(MODULE_NAME, "Volume mount manager initialized successfully");
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error initializing volume mount manager", 
                new AgentSystemException(e.getMessage(), e));
        }
    }
    
    /**
     * Loads the index file or creates it if it doesn't exist
     */
    private void loadIndex() {
        synchronized (indexLock) {
            try {
                Path indexFile = Paths.get(baseDirectory + INDEX_FILE);
                Path backupFile = Paths.get(baseDirectory + INDEX_FILE + ".bak");
                
                if (!Files.exists(indexFile)) {
                    LoggingService.logDebug(MODULE_NAME, "Creating new index file");
                    // Create new index file with empty data
                    indexData = Json.createObjectBuilder().build();
                    saveIndex();
                } else {
                    LoggingService.logDebug(MODULE_NAME, "Loading existing index file");
                    // Load existing index file
                    try (JsonReader reader = Json.createReader(new FileReader(indexFile.toFile()))) {
                        JsonObject fileData = reader.readObject();
                        String storedChecksum = fileData.getString("checksum");
                        JsonObject data = fileData.getJsonObject("data");
                        
                        // Verify checksum
                        String computedChecksum = checksum(data.toString());
                        if (!computedChecksum.equals(storedChecksum)) {
                            LoggingService.logError(MODULE_NAME, "Index file checksum verification failed", 
                                new AgentSystemException("Index file may have been tampered with"));
                            // Try to restore from backup
                            if (Files.exists(backupFile)) {
                                LoggingService.logInfo(MODULE_NAME, "Attempting to restore index from backup");
                                restoreIndexFromBackup();
                            } else {
                                // Initialize empty index if checksum fails and no backup
                                indexData = Json.createObjectBuilder().build();
                            }
                            return;
                        }
                        
                        indexData = data;
                        // Migrate old index format if needed
                        migrateIndexFormatIfNeeded();
                    }
                }
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME, "Error loading index file", 
                    new AgentSystemException(e.getMessage(), e));
                // Try to restore from backup
                restoreIndexFromBackup();
            }
        }
    }
    
    /**
     * Migrates old index format to new format (adds type and microservices fields)
     */
    private void migrateIndexFormatIfNeeded() {
        try {
            boolean needsMigration = false;
            JsonObjectBuilder newIndexBuilder = Json.createObjectBuilder();
            
            for (String uuid : indexData.keySet()) {
                JsonObject mountData = indexData.getJsonObject(uuid);
                if (mountData != null) {
                    JsonObjectBuilder mountBuilder = Json.createObjectBuilder(mountData);
                    
                    // Add type field if missing (default to secret for backward compatibility)
                    if (!mountData.containsKey("type")) {
                        mountBuilder.add("type", "secret");
                        needsMigration = true;
                    }
                    
                    // Add microservices array if missing
                    if (!mountData.containsKey("microservices")) {
                        mountBuilder.add("microservices", Json.createArrayBuilder().build());
                        needsMigration = true;
                    }
                    
                    newIndexBuilder.add(uuid, mountBuilder.build());
                } else {
                    newIndexBuilder.add(uuid, mountData);
                }
            }
            
            if (needsMigration) {
                LoggingService.logInfo(MODULE_NAME, "Migrating index format to new schema");
                indexData = newIndexBuilder.build();
                saveIndex();
            }
        } catch (Exception e) {
            LoggingService.logWarning(MODULE_NAME, "Error migrating index format: " + e.getMessage());
        }
    }
    
    /**
     * Restores index from backup file
     */
    private void restoreIndexFromBackup() {
        try {
            Path backupFile = Paths.get(baseDirectory + INDEX_FILE + ".bak");
            if (Files.exists(backupFile)) {
                LoggingService.logInfo(MODULE_NAME, "Restoring index from backup");
                try (JsonReader reader = Json.createReader(new FileReader(backupFile.toFile()))) {
                    JsonObject fileData = reader.readObject();
                    JsonObject data = fileData.getJsonObject("data");
                    indexData = data;
                    // Save restored index
                    saveIndex();
                }
            } else {
                // Initialize empty index if no backup available
                indexData = Json.createObjectBuilder().build();
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error restoring index from backup", 
                new AgentSystemException(e.getMessage(), e));
            // Initialize empty index if restore fails
            indexData = Json.createObjectBuilder().build();
        }
    }
    
    /**
     * Saves the index file atomically with backup
     */
    private void saveIndex() {
        synchronized (indexLock) {
            try {
                LoggingService.logDebug(MODULE_NAME, "Saving index file");
                Path indexFile = Paths.get(baseDirectory + INDEX_FILE);
                Path tempFile = Paths.get(baseDirectory + INDEX_FILE + ".tmp");
                Path backupFile = Paths.get(baseDirectory + INDEX_FILE + ".bak");
                
                // Create wrapper object with checksum and timestamp
                JsonObject wrapper = Json.createObjectBuilder()
                    .add("checksum", checksum(indexData.toString()))
                    .add("timestamp", System.currentTimeMillis())
                    .add("data", indexData)
                    .build();
                
                // Write to temp file
                try (JsonWriter writer = Json.createWriter(new FileWriter(tempFile.toFile()))) {
                    writer.writeObject(wrapper);
                }
                
                // Create backup of current index only if it exists and is different
                // Backup purpose: Recovery from corruption or failed writes
                if (Files.exists(indexFile)) {
                    // Only create backup if checksum is different (avoid identical files)
                    try {
                        try (JsonReader reader = Json.createReader(new FileReader(indexFile.toFile()))) {
                            JsonObject currentFileData = reader.readObject();
                            String currentChecksum = currentFileData.getString("checksum");
                            String newChecksum = wrapper.getString("checksum");
                            if (!currentChecksum.equals(newChecksum)) {
                                Files.copy(indexFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    } catch (Exception e) {
                        // If we can't compare, create backup anyway for safety
                        Files.copy(indexFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                
                // Atomic move temp to final location
                Files.move(tempFile, indexFile, 
                    StandardCopyOption.ATOMIC_MOVE, 
                    StandardCopyOption.REPLACE_EXISTING);
                
                // Update volume mount status
                StatusReporter.setVolumeMountManagerStatus(indexData.size(), System.currentTimeMillis());
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME, "Error saving index file", 
                    new AgentSystemException(e.getMessage(), e));
                // Restore from backup if available
                restoreIndexFromBackup();
            }
        }
    }
    
    /**
     * Rebuilds the type cache from index data
     */
    private void rebuildTypeCache() {
        synchronized (indexLock) {
            typeCache.clear();
            if (indexData != null) {
                indexData.forEach((uuid, mountDataValue) -> {
                    try {
                        JsonObject mountData = mountDataValue.asJsonObject();
                        String name = mountData.getString("name");
                        String type = mountData.getString("type", "secret");
                        typeCache.put(name, "secret".equals(type) ? VolumeMountType.SECRET : VolumeMountType.CONFIGMAP);
                    } catch (Exception e) {
                        LoggingService.logWarning(MODULE_NAME, "Error rebuilding type cache for UUID: " + uuid);
                    }
                });
            }
        }
    }
    
    /**
     * Gets the volume mount type by name (from cache)
     * @param volumeName The volume mount name
     * @return VolumeMountType or null if not found
     */
    public VolumeMountType getVolumeMountType(String volumeName) {
        return typeCache.get(volumeName);
    }
    
    /**
     * Gets volume mount info by name
     * @param volumeName The volume mount name
     * @return JsonObject with volume mount info or null if not found
     */
    public JsonObject getVolumeMountByName(String volumeName) {
        synchronized (indexLock) {
            if (indexData == null) {
                return null;
            }
            for (String uuid : indexData.keySet()) {
                JsonObject mountData = indexData.getJsonObject(uuid);
                if (mountData != null && mountData.getString("name", "").equals(volumeName)) {
                    return mountData;
                }
            }
            return null;
        }
    }
    
    /**
     * Processes volume mount changes from controller
     * @param volumeMounts Array of volume mount objects from controller
     */
    public void processVolumeMountChanges(JsonArray volumeMounts) {
        synchronized (indexLock) {
            try {
                LoggingService.logInfo(MODULE_NAME, "Processing volume mount changes");
                // Get existing volume mounts from index
                Set<String> existingUuids = indexData.keySet();
                
                // Get new volume mount UUIDs
                Set<String> newUuids = volumeMounts.stream()
                    .map(JsonValue::asJsonObject)
                    .map(obj -> obj.getString("uuid"))
                    .collect(Collectors.toSet());
                
                // Handle removed volume mounts
                existingUuids.stream()
                    .filter(uuid -> !newUuids.contains(uuid))
                    .forEach(this::deleteVolumeMount);
                
                // Handle new and updated volume mounts
                volumeMounts.forEach(mount -> {
                    JsonObject volumeMount = mount.asJsonObject();
                    String uuid = volumeMount.getString("uuid");
                    
                    if (existingUuids.contains(uuid)) {
                        LoggingService.logDebug(MODULE_NAME, "Updating volume mount: " + uuid);
                        updateVolumeMount(volumeMount);
                    } else {
                        LoggingService.logDebug(MODULE_NAME, "Creating new volume mount: " + uuid);
                        createVolumeMount(volumeMount);
                    }
                });
                
                // Rebuild type cache after updates
                rebuildTypeCache();
                
                // Save updated index (status will be updated by saveIndex)
                saveIndex();
                LoggingService.logInfo(MODULE_NAME, "Volume mount changes processed successfully");
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME, "Error processing volume mount changes", 
                    new AgentSystemException(e.getMessage(), e));
            }
        }
    }
    
    /**
     * Gets the directory name for a volume mount type
     * @param type VolumeMountType
     * @return Directory name (secrets or configMaps)
     */
    private String getTypeDirectory(VolumeMountType type) {
        return type == VolumeMountType.SECRET ? SECRETS_DIR : CONFIGMAPS_DIR;
    }
    
    /**
     * Gets the type prefix for per-microservice directories
     * @param type VolumeMountType
     * @return Type prefix (datasance.com~secret or datasance.com~configmap)
     */
    private String getTypePrefix(VolumeMountType type) {
        return type == VolumeMountType.SECRET ? "datasance.com~secret" : "datasance.com~configmap";
    }
    
    /**
     * Parses volume mount type from JSON
     * @param volumeMount Volume mount JSON object
     * @return VolumeMountType
     */
    private VolumeMountType parseVolumeMountType(JsonObject volumeMount) {
        String typeStr = volumeMount.getString("type", "secret");
        return "secret".equals(typeStr) ? VolumeMountType.SECRET : VolumeMountType.CONFIGMAP;
    }
    
    /**
     * Creates a versioned directory name
     * @return Versioned directory name (e.g., ..2025_01_01_01_01_10.123456789)
     */
    private String createVersionedDirectoryName() {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"));
        long nanoseconds = System.nanoTime() % 1_000_000_000;
        return String.format("..%s.%09d", timestamp, nanoseconds);
    }
    
    /**
     * Creates a new volume mount
     * @param volumeMount Volume mount object from controller
     */
    private void createVolumeMount(JsonObject volumeMount) {
        try {
            String uuid = volumeMount.getString("uuid");
            String name = volumeMount.getString("name");
            int version = volumeMount.getInt("version");
            JsonObject data = volumeMount.getJsonObject("data");
            VolumeMountType type = parseVolumeMountType(volumeMount);
            
            LoggingService.logDebug(MODULE_NAME, String.format("Creating volume mount - UUID: %s, Name: %s, Version: %d, Type: %s", 
                uuid, name, version, type));
            
            // Create type-specific directory structure
            String typeDir = getTypeDirectory(type);
            Path mountPath = Paths.get(baseDirectory, typeDir, name);
            Files.createDirectories(mountPath);
            setDirectoryPermissions(mountPath);

            // Create versioned directory
            String versionDirName = createVersionedDirectoryName();
            Path versionDir = mountPath.resolve(versionDirName);
            Files.createDirectories(versionDir);
            setDirectoryPermissions(versionDir);
            
            // Create files in versioned directory
            JsonObjectBuilder dataBuilder = Json.createObjectBuilder();
            data.forEach((key, value) -> {
                try {
                    String decodedContent = decodeBase64(value.toString());
                    Path filePath = versionDir.resolve(key);
                    // Create parent directories if they don't exist
                    Path parentDir = filePath.getParent();
                    if (parentDir != null) {
                        Files.createDirectories(parentDir);
                        setDirectoryPermissions(parentDir);
                    }
                    Files.write(filePath, decodedContent.getBytes());
                    // Set file permissions based on type
                    setFilePermissions(filePath, type);
                    // Store key in index
                    dataBuilder.add(key, key);
                } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Error creating file: " + key, 
                        new AgentSystemException(e.getMessage(), e));
                }
            });
            
            // Create ..data symlink pointing to versioned directory
            Path dataLink = mountPath.resolve(DATA_SYMLINK);
            if (Files.exists(dataLink)) {
                Files.delete(dataLink);
            }
            Files.createSymbolicLink(dataLink, Paths.get(versionDirName));
            setSymlinkPermissions(dataLink);
            
            // Create per-key symlinks
            data.forEach((key, value) -> {
                try {
                    Path keyLink = mountPath.resolve(key);
                    Path keyRelativePath = Paths.get(key);
                    if (Files.exists(keyLink)) {
                        Files.delete(keyLink);
                    }
                    Path parentDir = keyLink.getParent();
                    if (parentDir != null) {
                        Files.createDirectories(parentDir);
                        setDirectoryPermissions(parentDir);
                    }
                    Files.createSymbolicLink(keyLink, buildRelativeDataTarget(mountPath, keyLink, keyRelativePath));
                    setSymlinkPermissions(keyLink);
                } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Error creating symlink for key: " + key, 
                        new AgentSystemException(e.getMessage(), e));
                }
            });
            
            // Update index with new schema
            JsonArray microservicesArray = Json.createArrayBuilder().build();
            JsonObject mountData = Json.createObjectBuilder()
                .add("name", name)
                .add("type", type == VolumeMountType.SECRET ? "secret" : "configMap")
                .add("version", version)
                .add("data", dataBuilder)
                .add("microservices", microservicesArray)
                .build();
                
            indexData = Json.createObjectBuilder(indexData)
                .add(uuid, mountData)
                .build();
                
            saveIndex();
            LoggingService.logDebug(MODULE_NAME, "Volume mount created successfully: " + uuid);
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error creating volume mount", 
                new AgentSystemException(e.getMessage(), e));
        }
    }
    
    /**
     * Updates an existing volume mount
     * @param volumeMount Volume mount object from controller
     */
    private void updateVolumeMount(JsonObject volumeMount) {
        try {
            String uuid = volumeMount.getString("uuid");
            String name = volumeMount.getString("name");
            int version = volumeMount.getInt("version");
            JsonObject data = volumeMount.getJsonObject("data");
            VolumeMountType type = parseVolumeMountType(volumeMount);
            
            LoggingService.logDebug(MODULE_NAME, String.format("Updating volume mount - UUID: %s, Name: %s, Version: %d, Type: %s", 
                uuid, name, version, type));
            
            // Get current version from index
            JsonObject currentMount = indexData.getJsonObject(uuid);
            if (currentMount != null) {
                int currentVersion = currentMount.getInt("version");
                if (version <= currentVersion) {
                    LoggingService.logWarning(MODULE_NAME, 
                        String.format("Skipping update - new version %d not greater than current version %d", 
                        version, currentVersion));
                    return;
                }
            }
            
            // Get old keys for deletion
            Set<String> oldKeys = new HashSet<>();
            if (currentMount != null && currentMount.containsKey("data")) {
                JsonObject oldData = currentMount.getJsonObject("data");
                oldKeys.addAll(oldData.keySet());
            }
            Set<String> newKeys = new HashSet<>(data.keySet());
            Set<String> deletedKeys = new HashSet<>(oldKeys);
            deletedKeys.removeAll(newKeys);
            
            // Create type-specific directory structure
            String typeDir = getTypeDirectory(type);
            Path mountPath = Paths.get(baseDirectory, typeDir, name);
            Files.createDirectories(mountPath);
            setDirectoryPermissions(mountPath);

            // Create new versioned directory
            String versionDirName = createVersionedDirectoryName();
            Path versionDir = mountPath.resolve(versionDirName);
            Files.createDirectories(versionDir);
            setDirectoryPermissions(versionDir);

            // Create files in new versioned directory
            JsonObjectBuilder dataBuilder = Json.createObjectBuilder();
            data.forEach((key, value) -> {
                try {
                    String decodedContent = decodeBase64(value.toString());
                    Path filePath = versionDir.resolve(key);
                    // Create parent directories if they don't exist
                    Path parentDir = filePath.getParent();
                    if (parentDir != null) {
                        Files.createDirectories(parentDir);
                        setDirectoryPermissions(parentDir);
                    }
                    Files.write(filePath, decodedContent.getBytes());
                    // Set file permissions based on type
                    setFilePermissions(filePath, type);
                    // Store key in index
                    dataBuilder.add(key, key);
                } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Error updating file: " + key, 
                        new AgentSystemException(e.getMessage(), e));
                }
            });
            
            // Atomically swap ..data symlink to point to new version
            Path dataLink = mountPath.resolve(DATA_SYMLINK);
            Path newDataLink = mountPath.resolve(DATA_SYMLINK + ".tmp");
            if (Files.exists(newDataLink)) {
                Files.delete(newDataLink);
            }
            Files.createSymbolicLink(newDataLink, Paths.get(versionDirName));
            setSymlinkPermissions(newDataLink);
            Files.move(newDataLink, dataLink, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            
            // Update per-key symlinks (create new, update existing)
            data.forEach((key, value) -> {
                try {
                    Path keyLink = mountPath.resolve(key);
                    Path keyRelativePath = Paths.get(key);
                    if (Files.exists(keyLink)) {
                        Files.delete(keyLink);
                    }
                    Path parentDir = keyLink.getParent();
                    if (parentDir != null) {
                        Files.createDirectories(parentDir);
                        setDirectoryPermissions(parentDir);
                    }
                    Files.createSymbolicLink(keyLink, buildRelativeDataTarget(mountPath, keyLink, keyRelativePath));
                    setSymlinkPermissions(keyLink);
                } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Error updating symlink for key: " + key, 
                        new AgentSystemException(e.getMessage(), e));
                }
            });
            
            // Delete symlinks for removed keys
            deletedKeys.forEach(key -> {
                try {
                    Path keyLink = mountPath.resolve(key);
                    if (Files.exists(keyLink)) {
                        // Check if it's actually a symlink before deleting
                        if (Files.isSymbolicLink(keyLink)) {
                            Files.delete(keyLink);
                            LoggingService.logDebug(MODULE_NAME, "Deleted symlink for removed key: " + key);
                        } else {
                            // If it's not a symlink, still delete it (shouldn't happen, but be safe)
                            Files.delete(keyLink);
                            LoggingService.logWarning(MODULE_NAME, "Deleted non-symlink file for removed key: " + key);
                        }
                    }
                } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Error deleting symlink for removed key: " + key, 
                        new AgentSystemException(e.getMessage(), e));
                    // Don't just log warning - this is important, so log as error
                }
            });
            
            // Clean up old version directories (keep last MAX_VERSION_HISTORY)
            cleanupOldVersions(mountPath);
            
            // Get existing microservices list
            JsonArray microservicesArray = Json.createArrayBuilder().build();
            if (currentMount != null && currentMount.containsKey("microservices")) {
                microservicesArray = currentMount.getJsonArray("microservices");
            }
            
            // Update index with new schema
            JsonObject mountData = Json.createObjectBuilder()
                .add("name", name)
                .add("type", type == VolumeMountType.SECRET ? "secret" : "configMap")
                .add("version", version)
                .add("data", dataBuilder)
                .add("microservices", microservicesArray)
                .build();
                
            indexData = Json.createObjectBuilder(indexData)
                .add(uuid, mountData)
                .build();
                
            saveIndex();
            
            // Sync symlinks in per-microservice directories to reflect the update
            syncMicroserviceSymlinks(name, type);
            
            LoggingService.logDebug(MODULE_NAME, "Volume mount updated successfully: " + uuid);
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error updating volume mount", 
                new AgentSystemException(e.getMessage(), e));
        }
    }
    
    /**
     * Cleans up old version directories, keeping only the last MAX_VERSION_HISTORY versions
     * @param mountPath The mount path containing versioned directories
     */
    private void cleanupOldVersions(Path mountPath) {
        try {
            List<Path> versionDirs = new ArrayList<>();
            Files.list(mountPath).forEach(path -> {
                String fileName = path.getFileName().toString();
                if (fileName.startsWith("..") && fileName.matches("\\.\\.[0-9_]+\\.[0-9]+")) {
                    versionDirs.add(path);
                }
            });
            
            // Sort by modification time (oldest first)
            versionDirs.sort((p1, p2) -> {
                try {
                    return Long.compare(Files.getLastModifiedTime(p1).toMillis(), 
                                       Files.getLastModifiedTime(p2).toMillis());
                } catch (IOException e) {
                    return 0;
                }
            });
            
            // Delete old versions, keeping last MAX_VERSION_HISTORY
            int toDelete = versionDirs.size() - MAX_VERSION_HISTORY;
            for (int i = 0; i < toDelete && i < versionDirs.size(); i++) {
                try {
                    Files.walk(versionDirs.get(i))
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                LoggingService.logWarning(MODULE_NAME, "Error deleting old version: " + path);
                            }
                        });
                } catch (IOException e) {
                    LoggingService.logWarning(MODULE_NAME, "Error cleaning up old version: " + versionDirs.get(i));
                }
            }
        } catch (Exception e) {
            LoggingService.logWarning(MODULE_NAME, "Error during version cleanup: " + e.getMessage());
        }
    }
    
    /**
     * Recursively copies all files from source versioned directory to target, preserving directory structure.
     * Handles nested keys (e.g. xxx/yyy.creds).
     */
    private void copyVersionedDirRecursively(Path sourceVersionedDir, Path targetVersionedDir, VolumeMountType type) {
        try {
            Files.walk(sourceVersionedDir, Integer.MAX_VALUE)
                .filter(Files::isRegularFile)
                .forEach(sourceFile -> {
                    try {
                        Path relativePath = sourceVersionedDir.relativize(sourceFile);
                        Path targetFile = targetVersionedDir.resolve(relativePath);
                        Path parentDir = targetFile.getParent();
                        if (parentDir != null) {
                            Files.createDirectories(parentDir);
                        }
                        Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                        setFilePermissionsForBindMount(targetFile);
                    } catch (Exception e) {
                        LoggingService.logWarning(MODULE_NAME,
                            "Error copying file: " + sourceFile + " - " + e.getMessage());
                    }
                });
            setDirectoryPermissionsForBindMountRecursively(targetVersionedDir);
        } catch (IOException e) {
            LoggingService.logWarning(MODULE_NAME, "Error walking source versioned dir: " + e.getMessage());
        }
    }

    /**
     * Builds a symlink target to ..data/<key> relative to the symlink's parent directory.
     * This keeps nested keys valid (e.g. key: dir/file -> target: ../..data/dir/file).
     */
    private Path buildRelativeDataTarget(Path mountPath, Path keyLink, Path keyRelativePath) {
        Path keyParent = keyLink.getParent();
        if (keyParent == null) {
            keyParent = mountPath;
        }
        Path dataFilePath = mountPath.resolve(DATA_SYMLINK).resolve(keyRelativePath);
        return keyParent.relativize(dataFilePath);
    }
    
    /**
     * Recursively creates key symlinks under mountPath for every file in versionedDir.
     * Each file at versionedDir/relPath gets a symlink at mountPath/relPath -> ..data/relPath.
     * Handles nested keys (e.g. xxx/yyy.creds).
     */
    private void createKeySymlinksRecursively(Path mountPath, Path versionedDir) {
        try {
            Files.walk(versionedDir, Integer.MAX_VALUE)
                .filter(Files::isRegularFile)
                .forEach(filePath -> {
                    try {
                        Path relativePath = versionedDir.relativize(filePath);
                        Path keyLink = mountPath.resolve(relativePath);
                        if (Files.exists(keyLink)) {
                            Files.delete(keyLink);
                        }
                        Path parentDir = keyLink.getParent();
                        if (parentDir != null) {
                            Files.createDirectories(parentDir);
                            setDirectoryPermissionsForBindMount(parentDir);
                        }
                        Files.createSymbolicLink(keyLink, buildRelativeDataTarget(mountPath, keyLink, relativePath));
                        setSymlinkPermissions(keyLink);
                    } catch (Exception e) {
                        LoggingService.logWarning(MODULE_NAME,
                            "Error creating key symlink: " + filePath.getFileName() + " - " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            LoggingService.logWarning(MODULE_NAME, "Error walking versioned dir for symlinks: " + e.getMessage());
        }
    }
    
    /**
     * Removes symlinks under mountPath that no longer have a corresponding file in targetVersionedDir.
     * Walks recursively to handle nested key structure. Does not follow symlinks when walking.
     */
    private void removeObsoleteSymlinksRecursively(Path mountPath, Path targetVersionedDir) {
        try {
            List<Path> toDelete = new ArrayList<>();
            Files.walk(mountPath, Integer.MAX_VALUE)
                .filter(Files::isSymbolicLink)
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    if (fileName.equals(DATA_SYMLINK) || fileName.startsWith("..")) {
                        return;
                    }
                    Path relativePath = mountPath.relativize(path);
                    if (!Files.isRegularFile(targetVersionedDir.resolve(relativePath), LinkOption.NOFOLLOW_LINKS)) {
                        toDelete.add(path);
                    }
                });
            // Delete from deepest first so parent dirs are empty when we need to remove children
            toDelete.stream()
                .sorted(Comparator.comparingInt((Path p) -> p.getNameCount()).reversed())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        LoggingService.logDebug(MODULE_NAME, "Removed obsolete symlink: " + path);
                    } catch (Exception e) {
                        LoggingService.logWarning(MODULE_NAME, "Error removing obsolete symlink: " + path + " - " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            LoggingService.logWarning(MODULE_NAME, "Error walking mount path for obsolete symlinks: " + e.getMessage());
        }
    }
    
    /**
     * Sets file permissions based on volume mount type
     * @param path File path
     * @param type VolumeMountType
     */
    private void setFilePermissions(Path path, VolumeMountType type) {
        try {
            if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) {
                Set<PosixFilePermission> perms;
                if (type == VolumeMountType.SECRET) {
                    // Secrets: 600 (rw-------)
                    perms = PosixFilePermissions.fromString("rw-------");
                } else {
                    // ConfigMaps: 644 (rw-r--r--)
                    perms = PosixFilePermissions.fromString("rw-r--r--");
                }
                Files.setPosixFilePermissions(path, perms);
                
                // Try to set ownership to root:root (or current user if not root)
                try {
                    UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
                    GroupPrincipal group = lookupService.lookupPrincipalByGroupName("root");
                    PosixFileAttributeView fileAttributeView = Files.getFileAttributeView(path, 
                        PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
                    fileAttributeView.setGroup(group);
                } catch (Exception e) {
                    // Ignore if we can't set ownership (e.g., not running as root)
                    LoggingService.logDebug(MODULE_NAME, "Could not set file ownership: " + e.getMessage());
                }
            } else if (SystemUtils.IS_OS_WINDOWS) {
                DosFileAttributeView fileAttributeView = Files.getFileAttributeView(path, 
                    DosFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
                fileAttributeView.setReadOnly(type == VolumeMountType.SECRET);
            }
        } catch (Exception e) {
            LoggingService.logWarning(MODULE_NAME, "Error setting file permissions: " + e.getMessage());
        }
    }
    
    /**
     * Sets symlink permissions to 777 (traversal permissions)
     * @param symlink Symlink path
     */
    private void setSymlinkPermissions(Path symlink) {
        try {
            if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) {
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrwxrwx");
                Files.setPosixFilePermissions(symlink, perms);
            }
        } catch (Exception e) {
            LoggingService.logWarning(MODULE_NAME, "Error setting symlink permissions: " + e.getMessage());
        }
    }

    /**
     * Sets directory permissions to 750 (rwxr-x---) for security.
     * Restricts access to owner and group only, matching Kubernetes kubelet pattern.
     * @param path Directory path
     */
    private void setDirectoryPermissions(Path path) {
        try {
            if ((SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) && Files.exists(path) && Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-x---");
                Files.setPosixFilePermissions(path, perms);
            }
        } catch (Exception e) {
            LoggingService.logWarning(MODULE_NAME, "Could not set directory permissions: " + e.getMessage());
        }
    }

    /**
     * Recursively sets 750 permissions on a directory and all its descendants.
     * Used after copyVersionedDirRecursively to secure nested directory structures.
     */
    private void setDirectoryPermissionsRecursively(Path dir) {
        try {
            if (!(SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) || !Files.exists(dir) || !Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) {
                return;
            }
            Files.walk(dir, Integer.MAX_VALUE)
                .filter(p -> Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS))
                .forEach(this::setDirectoryPermissions);
        } catch (IOException e) {
            LoggingService.logWarning(MODULE_NAME, "Could not set directory permissions recursively: " + e.getMessage());
        }
    }

    /**
     * Sets directory permissions to 755 (rwxr-xr-x) for bind-mount targets.
     * Allows non-root container processes to traverse and read mounted volumes.
     * @param path Directory path
     */
    private void setDirectoryPermissionsForBindMount(Path path) {
        try {
            if ((SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) && Files.exists(path) && Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-xr-x");
                Files.setPosixFilePermissions(path, perms);
            }
        } catch (Exception e) {
            LoggingService.logWarning(MODULE_NAME, "Could not set bind-mount directory permissions: " + e.getMessage());
        }
    }

    /**
     * Recursively sets 755 permissions on a directory and all its descendants.
     * Used for per-microservice bind-mount target so non-root containers can access.
     */
    private void setDirectoryPermissionsForBindMountRecursively(Path dir) {
        try {
            if (!(SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) || !Files.exists(dir) || !Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) {
                return;
            }
            Files.walk(dir, Integer.MAX_VALUE)
                .filter(p -> Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS))
                .forEach(this::setDirectoryPermissionsForBindMount);
        } catch (IOException e) {
            LoggingService.logWarning(MODULE_NAME, "Could not set bind-mount directory permissions recursively: " + e.getMessage());
        }
    }

    /**
     * Sets file permissions to 644 (rw-r--r--) for bind-mount targets.
     * Allows non-root container processes to read both secrets and configmaps.
     * @param path File path
     */
    private void setFilePermissionsForBindMount(Path path) {
        try {
            if ((SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) && Files.exists(path) && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-r--r--");
                Files.setPosixFilePermissions(path, perms);
            }
        } catch (Exception e) {
            LoggingService.logWarning(MODULE_NAME, "Could not set bind-mount file permissions: " + e.getMessage());
        }
    }

    /**
     * Deletes a volume mount
     * @param uuid UUID of the volume mount to delete
     */
    private void deleteVolumeMount(String uuid) {
        try {
            LoggingService.logDebug(MODULE_NAME, "Deleting volume mount: " + uuid);
            
            // Get mount info from index
            JsonObject mountData = indexData.getJsonObject(uuid);
            if (mountData == null) {
                LoggingService.logWarning(MODULE_NAME, "Volume mount not found: " + uuid);
                return;
            }
            
            // Delete mount directory and files
            String name = mountData.getString("name");
            String typeStr = mountData.getString("type", "secret");
            String typeDir = "secret".equals(typeStr) ? SECRETS_DIR : CONFIGMAPS_DIR;
            Path mountPath = Paths.get(baseDirectory, typeDir, name);
            if (Files.exists(mountPath)) {
                Files.walk(mountPath)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            LoggingService.logError(MODULE_NAME, "Error deleting file: " + path, 
                                new AgentSystemException(e.getMessage(), e));
                        }
                    });
            }
            
            // Remove from index
            JsonObjectBuilder newIndexBuilder = Json.createObjectBuilder();
            indexData.forEach((key, value) -> {
                if (!key.equals(uuid)) {
                    newIndexBuilder.add(key, value);
                }
            });
            indexData = newIndexBuilder.build();
            
            // Remove from type cache
            typeCache.remove(name);
            
            saveIndex();
            LoggingService.logDebug(MODULE_NAME, "Volume mount deleted successfully: " + uuid);
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error deleting volume mount", 
                new AgentSystemException(e.getMessage(), e));
        }
    }
    
    /**
     * Prepares per-microservice volume mount directory with symlinks
     * Fast path: returns immediately if directory exists
     * @param microserviceUuid Microservice UUID
     * @param volumeName Volume mount name
     * @param type VolumeMountType
     * @return Path to per-microservice mount directory
     */
    public String prepareMicroserviceVolumeMount(String microserviceUuid, String volumeName, VolumeMountType type) {
        try {
            Path mountPath = getMountPath(microserviceUuid, volumeName, type);
            
            // Fast path: if exists and has symlinks, skip (zero overhead)
            if (Files.exists(mountPath) && hasSymlinks(mountPath)) {
                return mountPath.toString();
            }
            
            // Slow path: create directory and copy files (only on first creation)
            try {
                // Atomic directory creation (handles race conditions)
                Files.createDirectories(mountPath);
                setDirectoryPermissionsForBindMount(mountPath);

                // Calculate source path
                String typeDir = getTypeDirectory(type);
                Path sourcePath = Paths.get(baseDirectory, typeDir, volumeName);
                Path sourceDataLink = sourcePath.resolve(DATA_SYMLINK);
                
                // Resolve the actual ..data symlink to get the real versioned directory
                Path sourceVersionedDir;
                if (Files.exists(sourceDataLink)) {
                    sourceVersionedDir = sourceDataLink.toRealPath();
                } else {
                    LoggingService.logWarning(MODULE_NAME, "Source ..data symlink does not exist for: " + volumeName);
                    return mountPath.toString(); // Return path anyway
                }
                
                // Get the versioned directory name (e.g., ..2025_12_30_15_00_00.123456789)
                String versionedDirName = sourceVersionedDir.getFileName().toString();
                
                // Copy the versioned directory to per-microservice directory (recursive for nested keys)
                Path targetVersionedDir = mountPath.resolve(versionedDirName);
                if (!Files.exists(targetVersionedDir)) {
                    Files.createDirectories(targetVersionedDir);
                    setDirectoryPermissionsForBindMount(targetVersionedDir);
                    copyVersionedDirRecursively(sourceVersionedDir, targetVersionedDir, type);
                }
                
                // Create ..data symlink pointing to versioned directory (relative path)
                Path dataLink = mountPath.resolve(DATA_SYMLINK);
                if (!Files.exists(dataLink)) {
                    // Use relative path for ..data symlink (works in containers)
                    Files.createSymbolicLink(dataLink, Paths.get(versionedDirName));
                    setSymlinkPermissions(dataLink);
                }
                
                // Create key symlinks recursively (handles nested keys e.g. sys/admin-hub.creds)
                createKeySymlinksRecursively(mountPath, targetVersionedDir);
                
                // Track microservice usage in index
                trackMicroserviceUsage(volumeName, microserviceUuid, true);
                
            } catch (Exception e) {
                // Log error but don't fail container creation
                LoggingService.logWarning(MODULE_NAME, 
                    "Error creating volume mount structure: " + e.getMessage());
                // Return path anyway - background thread will fix
            }
            
            return mountPath.toString();
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error preparing microservice volume mount", 
                new AgentSystemException(e.getMessage(), e));
            // Return path anyway to avoid blocking container creation
            return getMountPath(microserviceUuid, volumeName, type).toString();
        }
    }
    
    /**
     * Gets the mount path for a microservice volume mount
     * @param microserviceUuid Microservice UUID
     * @param volumeName Volume mount name
     * @param type VolumeMountType
     * @return Path to per-microservice mount directory
     */
    private Path getMountPath(String microserviceUuid, String volumeName, VolumeMountType type) {
        String typePrefix = getTypePrefix(type);
        return Paths.get(baseDirectory, MICROSERVICES_DIR, microserviceUuid, "volumes", typePrefix, volumeName);
    }
    
    /**
     * Syncs per-microservice symlinks when source volume mount is updated
     * @param volumeName Volume mount name
     * @param type VolumeMountType
     */
    private void syncMicroserviceSymlinks(String volumeName, VolumeMountType type) {
        synchronized (indexLock) {
            try {
                JsonObject volumeMountData = getVolumeMountByName(volumeName);
                if (volumeMountData == null) {
                    return;
                }
                
                JsonArray microservicesArray = volumeMountData.getJsonArray("microservices");
                if (microservicesArray == null) {
                    return;
                }
                
                // Get the source versioned directory
                String typeDir = getTypeDirectory(type);
                Path sourcePath = Paths.get(baseDirectory, typeDir, volumeName);
                Path sourceDataLink = sourcePath.resolve(DATA_SYMLINK);
                
                if (!Files.exists(sourceDataLink)) {
                    return;
                }
                
                Path sourceVersionedDir = sourceDataLink.toRealPath();
                String versionedDirName = sourceVersionedDir.getFileName().toString();
                
                // Update symlinks for each microservice using this volume mount
                for (int i = 0; i < microservicesArray.size(); i++) {
                    String microserviceUuid = microservicesArray.getString(i);
                    Path mountPath = getMountPath(microserviceUuid, volumeName, type);
                    
                    if (!Files.exists(mountPath)) {
                        continue;
                    }
                    
                    // Copy new versioned directory if it doesn't exist (recursive for nested keys)
                    Path targetVersionedDir = mountPath.resolve(versionedDirName);
                    if (!Files.exists(targetVersionedDir)) {
                        Files.createDirectories(targetVersionedDir);
                        setDirectoryPermissionsForBindMount(targetVersionedDir);
                        copyVersionedDirRecursively(sourceVersionedDir, targetVersionedDir, type);
                    }
                    
                    // Atomically update ..data symlink to point to new version
                    Path dataLink = mountPath.resolve(DATA_SYMLINK);
                    Path newDataLink = mountPath.resolve(DATA_SYMLINK + ".tmp");
                    if (Files.exists(newDataLink)) {
                        Files.delete(newDataLink);
                    }
                    Files.createSymbolicLink(newDataLink, Paths.get(versionedDirName));
                    setSymlinkPermissions(newDataLink);
                    Files.move(newDataLink, dataLink, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                    
                    // Update/create key symlinks recursively (handles nested keys)
                    createKeySymlinksRecursively(mountPath, targetVersionedDir);
                    
                    // Remove symlinks for keys that no longer exist (recursive for nested structure)
                    removeObsoleteSymlinksRecursively(mountPath, targetVersionedDir);
                    
                    // Clean up old versioned directories in per-microservice directory
                    cleanupOldVersions(mountPath);
                }
            } catch (Exception e) {
                LoggingService.logWarning(MODULE_NAME, "Error syncing microservice symlinks: " + e.getMessage());
            }
        }
    }
    
    /**
     * Checks if mount path has symlinks (fast path check)
     * @param mountPath Mount path to check
     * @return true if ..data symlink exists
     */
    private boolean hasSymlinks(Path mountPath) {
        return Files.exists(mountPath.resolve(DATA_SYMLINK));
    }
    
    /**
     * Tracks microservice usage of volume mounts in index
     * @param volumeName Volume mount name
     * @param microserviceUuid Microservice UUID
     * @param add true to add, false to remove
     */
    private void trackMicroserviceUsage(String volumeName, String microserviceUuid, boolean add) {
        synchronized (indexLock) {
            try {
                // Find volume mount by name
                for (String uuid : indexData.keySet()) {
                    JsonObject mountData = indexData.getJsonObject(uuid);
                    if (mountData != null && mountData.getString("name", "").equals(volumeName)) {
                        JsonArray microservicesArray = mountData.getJsonArray("microservices");
                        List<String> microservices = new ArrayList<>();
                        
                        // Convert to list
                        if (microservicesArray != null) {
                            for (int i = 0; i < microservicesArray.size(); i++) {
                                microservices.add(microservicesArray.getString(i));
                            }
                        }
                        
                        if (add && !microservices.contains(microserviceUuid)) {
                            microservices.add(microserviceUuid);
                        } else if (!add) {
                            microservices.remove(microserviceUuid);
                        }
                        
                        // Rebuild mount data with updated microservices list
                        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                        microservices.forEach(arrayBuilder::add);
                        
                        JsonObjectBuilder mountBuilder = Json.createObjectBuilder(mountData);
                        mountBuilder.remove("microservices");
                        mountBuilder.add("microservices", arrayBuilder.build());
                        
                        JsonObjectBuilder indexBuilder = Json.createObjectBuilder(indexData);
                        indexBuilder.remove(uuid);
                        indexBuilder.add(uuid, mountBuilder.build());
                        indexData = indexBuilder.build();
                        
                        break;
                    }
                }
            } catch (Exception e) {
                LoggingService.logWarning(MODULE_NAME, 
                    "Error tracking microservice usage: " + e.getMessage());
            }
        }
    }
    
    /**
     * Cleans up per-microservice volume mount directories
     * @param microserviceUuid Microservice UUID
     */
    public void cleanupMicroserviceVolumes(String microserviceUuid) {
        try {
            LoggingService.logDebug(MODULE_NAME, "Cleaning up microservice volumes: " + microserviceUuid);
            
            Path microservicePath = Paths.get(baseDirectory, MICROSERVICES_DIR, microserviceUuid);
            if (Files.exists(microservicePath)) {
                // Find all volume mounts used by this microservice from index
                synchronized (indexLock) {
                    for (String uuid : indexData.keySet()) {
                        JsonObject mountData = indexData.getJsonObject(uuid);
                        if (mountData != null && mountData.containsKey("microservices")) {
                            JsonArray microservicesArray = mountData.getJsonArray("microservices");
                            if (microservicesArray != null) {
                                for (int i = 0; i < microservicesArray.size(); i++) {
                                    if (microserviceUuid.equals(microservicesArray.getString(i))) {
                                        String volumeName = mountData.getString("name");
                                        // Remove from tracking
                                        trackMicroserviceUsage(volumeName, microserviceUuid, false);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Delete per-microservice mount directory
                Files.walk(microservicePath)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            LoggingService.logWarning(MODULE_NAME, "Error deleting microservice volume: " + path);
                        }
                    });
            }
            
            LoggingService.logDebug(MODULE_NAME, "Microservice volumes cleaned up: " + microserviceUuid);
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error cleaning up microservice volumes", 
                new AgentSystemException(e.getMessage(), e));
        }
    }
    
    /**
     * Decodes a base64 encoded string
     * @param encoded Base64 encoded string
     * @return Decoded string
     */
    private String decodeBase64(String encoded) {
        try {
            // Remove quotes if present
            String cleanEncoded = encoded.replaceAll("^\"|\"$", "");
            byte[] decodedBytes = Base64.getDecoder().decode(cleanEncoded);
            return new String(decodedBytes);
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error decoding base64 string", 
                new AgentSystemException(e.getMessage(), e));
            return "";
        }
    }

    private String checksum(String data) {
        try {
            // Only compute checksum on the structure, not the content
            byte[] base64 = Base64.getEncoder().encode(data.getBytes(StandardCharsets.UTF_8));
            MessageDigest md = MessageDigest.getInstance("SHA1");
            md.update(base64);
            byte[] mdbytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte mdbyte : mdbytes) {
                sb.append(Integer.toString((mdbyte & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error computing checksum", 
                new AgentSystemException(e.getMessage(), e));
            return "";
        }
    }

    /**
     * Clears all volume mounts and their associated files
     * Used during deprovisioning
     */
    public void clear() {
        synchronized (indexLock) {
            try {
                LoggingService.logDebug(MODULE_NAME, "Start clearing volume mounts");
                
                // Delete all volume mount directories (secrets, configMaps, microservices)
                Path volumesPath = Paths.get(baseDirectory);
                if (Files.exists(volumesPath)) {
                    Files.walk(volumesPath)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                // Don't delete the base directory itself
                                if (!path.equals(volumesPath)) {
                                    Files.delete(path);
                                }
                            } catch (IOException e) {
                                LoggingService.logWarning(MODULE_NAME, "Error deleting: " + path);
                            }
                        });
                }
                
                // Delete index file and backups
                Path indexFile = Paths.get(baseDirectory + INDEX_FILE);
                if (Files.exists(indexFile)) {
                    Files.delete(indexFile);
                }
                Path backupFile = Paths.get(baseDirectory + INDEX_FILE + ".bak");
                if (Files.exists(backupFile)) {
                    Files.delete(backupFile);
                }
                Path tempFile = Paths.get(baseDirectory + INDEX_FILE + ".tmp");
                if (Files.exists(tempFile)) {
                    Files.delete(tempFile);
                }
                
                // Clear index data and cache
                indexData = Json.createObjectBuilder().build();
                typeCache.clear();
                
                // Update status reporter
                StatusReporter.setVolumeMountManagerStatus(0, System.currentTimeMillis());
                
                LoggingService.logDebug(MODULE_NAME, "Finished clearing volume mounts");
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME, "Error clearing volume mounts", 
                    new AgentSystemException(e.getMessage(), e));
            }
        }
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                }
                file.delete();
            }
        }
        directory.delete();
    }
} 