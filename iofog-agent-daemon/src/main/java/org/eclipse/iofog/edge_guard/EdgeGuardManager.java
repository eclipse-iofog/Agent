package org.eclipse.iofog.edge_guard;

import org.eclipse.iofog.field_agent.FieldAgent;
import org.eclipse.iofog.utils.configuration.Configuration;
import org.eclipse.iofog.utils.Constants;
import org.eclipse.iofog.utils.logging.LoggingService;
import org.eclipse.iofog.status_reporter.StatusReporter;

import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.JWTClaimsSet;

import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.OperatingSystem;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.Locale;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;
import java.io.File;

import static org.eclipse.iofog.utils.Constants.ControllerStatus.NOT_PROVISIONED;

public class EdgeGuardManager {
    private static final String MODULE_NAME = "Edge Guard";
    private static EdgeGuardManager instance;
    private ScheduledExecutorService scheduler = null;
    private ScheduledFuture<?> futureTask;
    private static Ed25519Signer signer;
    private static OctetKeyPair keyPair;

    private EdgeGuardManager() {
        // Removed initialization from constructor
    }

    public static EdgeGuardManager getInstance() {
        if (instance == null) {
            synchronized (EdgeGuardManager.class) {
                if (instance == null) {
                    instance = new EdgeGuardManager();
                }
            }
        }
        return instance;
    }

    public void start() throws Exception {
        LoggingService.logInfo(MODULE_NAME, "Start edge guard manager");
        scheduler = Executors.newScheduledThreadPool(1);
        long edgeGuardFrequency = Configuration.getEdgeGuardFrequency();
        if (edgeGuardFrequency > 0 ) {
            futureTask = scheduler.scheduleAtFixedRate(
                triggerEdgeGuardFrequency, 
                edgeGuardFrequency, 
                edgeGuardFrequency, 
                TimeUnit.SECONDS
            );
            LoggingService.logInfo(MODULE_NAME, "Edge guard manager started with frequency: " + edgeGuardFrequency + " seconds");
        }
    }

    public void changeEdgeGuardFreqInterval() {
        if (futureTask != null) {
            futureTask.cancel(true);
            futureTask = null;
        }
        
        long edgeGuardFrequency = Configuration.getEdgeGuardFrequency();
        if (edgeGuardFrequency > 0 ) {
            futureTask = scheduler.scheduleAtFixedRate(
                triggerEdgeGuardFrequency, 
                edgeGuardFrequency, 
                edgeGuardFrequency, 
                TimeUnit.SECONDS
            );
            LoggingService.logInfo(MODULE_NAME, "Edge guard frequency updated to: " + edgeGuardFrequency + " seconds");
        }
    }

    private final Runnable triggerEdgeGuardFrequency = () -> {
        boolean notProvisioned = StatusReporter.getFieldAgentStatus().getControllerStatus().equals(NOT_PROVISIONED);
        if (notProvisioned) {
            LoggingService.logInfo(MODULE_NAME, "Agent is not provisioned, skipping edge guard check");
            return;
        } else {
            try {
                LoggingService.logInfo(MODULE_NAME, "Checking hardware signature");
                checkHardwareSignature();
            } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Error checking hardware signature", e);
                }
        }
    };


    private void checkHardwareSignature() {
        try {
            LoggingService.logDebug(MODULE_NAME, "Started checking hardware signature");
            
            String newSignature = collectAndSignHardwareSignature();
            if (newSignature == null) {
                LoggingService.logError(MODULE_NAME, "Failed to collect hardware signature", new Exception("Failed to collect hardware signature"));
                return;
            }
            
            String storedSignature = Configuration.getHwSignature();
            LoggingService.logDebug(MODULE_NAME, "Stored signature: " + storedSignature);
            LoggingService.logDebug(MODULE_NAME, "New signature: " + newSignature);
            
            if (storedSignature == null) {
                // First run - store signature
                LoggingService.logInfo(MODULE_NAME, "Storing initial hardware signature");
                Configuration.setHwSignature(newSignature);
                return; // Exit after storing initial signature
            }
            
            if (!newSignature.equals(storedSignature)) {
                LoggingService.logWarning(MODULE_NAME, "Hardware signature mismatch detected");
                LoggingService.logDebug(MODULE_NAME, "Signature comparison failed - stored vs new are different");
                handleHardwareChange();
            } else {
                LoggingService.logDebug(MODULE_NAME, "Hardware signature verification successful - signatures match");
            }
            
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error checking hardware signature", e);
        }
    }

    private void handleHardwareChange() {
        try {
            // Update daemon status to WARNING
            StatusReporter.setSupervisorStatus().setDaemonStatus(Constants.ModulesStatus.WARNING);
            StatusReporter.setSupervisorStatus().setWarningMessage("HW signature changed");
            
            // Immediately send status to controller
            FieldAgent.getInstance().postStatusHelper();
            
            // Deprovision the agent
            FieldAgent.getInstance().deProvision(false);
            
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error handling hardware change", e);
        }
    }

    private void clearHardwareSignature() {
        try {
            Configuration.setHwSignature(null);
            LoggingService.logInfo(MODULE_NAME, "Cleared hardware signature");
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error clearing hardware signature", e);
        }
    }


    private String collectAndSignHardwareSignature() {
        SystemInfo systemInfo = null;
        HardwareAbstractionLayer hal = null;
        long startTime = System.currentTimeMillis();
        
        try {
            LoggingService.logDebug(MODULE_NAME, "Starting hardware signature collection");
            
            // Initialize SystemInfo with timing
            LoggingService.logDebug(MODULE_NAME, "Initializing SystemInfo...");
            long sysInfoStart = System.currentTimeMillis();
            systemInfo = new SystemInfo();
            LoggingService.logDebug(MODULE_NAME, "SystemInfo initialized in " + (System.currentTimeMillis() - sysInfoStart) + "ms");
            
            // Initialize HardwareAbstractionLayer with timing
            LoggingService.logDebug(MODULE_NAME, "Initializing HardwareAbstractionLayer...");
            long halStart = System.currentTimeMillis();
            hal = systemInfo.getHardware();
            LoggingService.logDebug(MODULE_NAME, "HardwareAbstractionLayer initialized in " + (System.currentTimeMillis() - halStart) + "ms");
            
            StringBuilder hardwareData = new StringBuilder();
            
            // Collect each component with individual timeouts and error handling
            collectCpuInfo(hardwareData, hal);
            collectSystemInfo(hardwareData, hal);
            collectMemoryInfo(hardwareData, hal);
            collectStorageInfo(hardwareData, hal);
            collectPciInfo(hardwareData, hal);
            collectNetworkInfo(hardwareData, hal);
            collectUsbInfo(hardwareData, hal);
            collectOsInfo(hardwareData, systemInfo);
            // collectPowerInfo(hardwareData, hal);
            // collectSensorInfo(hardwareData, hal);
            
            String data = hardwareData.toString();
            LoggingService.logDebug(MODULE_NAME, "Collected hardware data: " + data);
            
            String hash = hashData(data);
            String signature = signWithPrivateKey(hash);
            
            LoggingService.logDebug(MODULE_NAME, "Hardware signature collection completed in " + 
                (System.currentTimeMillis() - startTime) + "ms");
            
            return signature;
            
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error collecting hardware signature after " + 
                (System.currentTimeMillis() - startTime) + "ms", e);
            return null;
        } 
    }

    private void collectCpuInfo(StringBuilder hardwareData, HardwareAbstractionLayer hal) {
        try {
            LoggingService.logDebug(MODULE_NAME, "Collecting CPU information");
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    hardwareData.append("=== CPU Information ===\n");
                    CentralProcessor cpu = hal.getProcessor();
                    CentralProcessor.ProcessorIdentifier processorId = cpu.getProcessorIdentifier();
                    
                    hardwareData.append("Architecture: ").append(processorId.getMicroarchitecture()).append("\n");
                    hardwareData.append("CPU(s): ").append(cpu.getLogicalProcessorCount()).append("\n");
                    hardwareData.append("Thread(s) per core: ").append(cpu.getLogicalProcessorCount() / cpu.getPhysicalProcessorCount()).append("\n");
                    hardwareData.append("Core(s) per socket: ").append(cpu.getPhysicalProcessorCount()).append("\n");
                    hardwareData.append("Socket(s): ").append(cpu.getPhysicalPackageCount()).append("\n");
                    hardwareData.append("Model name: ").append(processorId.getName()).append("\n");
                    hardwareData.append("CPU family: ").append(processorId.getFamily()).append("\n");
                    hardwareData.append("Model: ").append(processorId.getModel()).append("\n");
                    hardwareData.append("Stepping: ").append(processorId.getStepping()).append("\n");
                    // hardwareData.append("CPU MHz: ").append(cpu.getMaxFreq() / 1_000_000).append("\n");
                    hardwareData.append("Vendor ID: ").append(processorId.getVendor()).append("\n");
                    hardwareData.append("Processor ID: ").append(processorId.getProcessorID()).append("\n");
                    hardwareData.append("Flags: ").append(String.join(" ", cpu.getFeatureFlags())).append("\n");
                    
                    // Cache information
                    // if (!cpu.getProcessorCaches().isEmpty()) {
                    //     hardwareData.append("L1d cache: ").append(formatBytes(cpu.getProcessorCaches().get(0).getCacheSize())).append("\n");
                    //     if (cpu.getProcessorCaches().size() > 1) {
                    //         hardwareData.append("L1i cache: ").append(formatBytes(cpu.getProcessorCaches().get(1).getCacheSize())).append("\n");
                    //     }
                    //     if (cpu.getProcessorCaches().size() > 2) {
                    //         hardwareData.append("L2 cache: ").append(formatBytes(cpu.getProcessorCaches().get(2).getCacheSize())).append("\n");
                    //     }
                    //     if (cpu.getProcessorCaches().size() > 3) {
                    //         hardwareData.append("L3 cache: ").append(formatBytes(cpu.getProcessorCaches().get(3).getCacheSize())).append("\n");
                    //     }
                    // }
                    for (CentralProcessor.ProcessorCache cache : cpu.getProcessorCaches()) {
                        String cacheType = cache.getType().toString() + "_L" + cache.getLevel();
                        hardwareData.append(cacheType).append(" cache: ")
                                   .append(formatBytes(cache.getCacheSize())).append("\n");
                    }

                } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Error collecting CPU information", e);
                    hardwareData.append("CPU Information: Error collecting data\n");
                }
            });
            
            try {
                future.get(3, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                LoggingService.logError(MODULE_NAME, "CPU information collection timed out", e);
                hardwareData.append("CPU Information: Collection timed out\n");
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME, "Error in CPU information collection", e);
                hardwareData.append("CPU Information: Error collecting data\n");
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error in CPU information collection", e);
            hardwareData.append("CPU Information: Error collecting data\n");
        }
    }

    private void collectSystemInfo(StringBuilder hardwareData, HardwareAbstractionLayer hal) {
        try {
            LoggingService.logDebug(MODULE_NAME, "Collecting system information");
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    hardwareData.append("\n=== System Hardware ===\n");
                    ComputerSystem cs = hal.getComputerSystem();
                    
                    hardwareData.append("System:\n");
                    hardwareData.append("  Manufacturer: ").append(cs.getManufacturer()).append("\n");
                    hardwareData.append("  Model: ").append(cs.getModel()).append("\n");
                    hardwareData.append("  Serial: ").append(cs.getSerialNumber()).append("\n");
                    hardwareData.append("  UUID: ").append(cs.getHardwareUUID()).append("\n");
                    
                    Baseboard baseboard = cs.getBaseboard();
                    hardwareData.append("Motherboard:\n");
                    hardwareData.append("  Manufacturer: ").append(baseboard.getManufacturer()).append("\n");
                    hardwareData.append("  Model: ").append(baseboard.getModel()).append("\n");
                    hardwareData.append("  Version: ").append(baseboard.getVersion()).append("\n");
                    hardwareData.append("  Serial: ").append(baseboard.getSerialNumber()).append("\n");
                    
                    Firmware firmware = cs.getFirmware();
                    hardwareData.append("BIOS/UEFI:\n");
                    hardwareData.append("  Manufacturer: ").append(firmware.getManufacturer()).append("\n");
                    // hardwareData.append("  Version: ").append(firmware.getVersion()).append("\n");
                    // hardwareData.append("  Release Date: ").append(firmware.getReleaseDate()).append("\n");
                } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Error collecting system information", e);
                    hardwareData.append("System Information: Error collecting data\n");
                }
            });
            
            try {
                future.get(3, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                LoggingService.logError(MODULE_NAME, "System information collection timed out", e);
                hardwareData.append("System Information: Collection timed out\n");
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME, "Error in system information collection", e);
                hardwareData.append("System Information: Error collecting data\n");
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error in system information collection", e);
            hardwareData.append("System Information: Error collecting data\n");
        }
    }

    private void collectMemoryInfo(StringBuilder hardwareData, HardwareAbstractionLayer hal) {
        try {
            LoggingService.logDebug(MODULE_NAME, "Collecting memory information");
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    GlobalMemory memory = hal.getMemory();
                    hardwareData.append("Memory:\n");
                    hardwareData.append("  Total: ").append(formatBytes(memory.getTotal())).append("\n");
                    hardwareData.append("  Physical Memory Modules:\n");
                    for (PhysicalMemory pm : memory.getPhysicalMemory()) {
                        hardwareData.append("    ").append(pm.toString()).append("\n");
                    }
                } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Error collecting memory information", e);
                    hardwareData.append("Memory Information: Error collecting data\n");
                }
            });
            
            try {
                future.get(3, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                LoggingService.logError(MODULE_NAME, "Memory information collection timed out", e);
                hardwareData.append("Memory Information: Collection timed out\n");
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME, "Error in memory information collection", e);
                hardwareData.append("Memory Information: Error collecting data\n");
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error in memory information collection", e);
            hardwareData.append("Memory Information: Error collecting data\n");
        }
    }

    private void collectStorageInfo(StringBuilder hardwareData, HardwareAbstractionLayer hal) {
        try {
            LoggingService.logDebug(MODULE_NAME, "Collecting storage information");
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    hardwareData.append("Storage Devices:\n");
                    List<HWDiskStore> disks = hal.getDiskStores();
                    
                    if (disks.isEmpty()) {
                        // Fall back to direct filesystem access for physical devices
                        File blockDir = new File("/sys/block");
                        if (blockDir.exists() && blockDir.isDirectory()) {
                            File[] devices = blockDir.listFiles();
                            if (devices != null) {
                                for (File device : devices) {
                                    String deviceName = device.getName();
                                    
                                    // Skip virtual and special devices
                                    if (deviceName.startsWith("vd") ||    // Virtual disks
                                        deviceName.startsWith("loop") ||  // Loop devices
                                        deviceName.startsWith("ram") ||   // RAM disks
                                        deviceName.startsWith("dm-") ||   // Device mapper
                                        deviceName.contains("boot")) {    // Boot partitions
                                        continue;
                                    }

                                    // Only process physical storage devices
                                    if (deviceName.startsWith("sd") ||    // SATA/SCSI
                                        deviceName.startsWith("mmc") ||   // SD/eMMC
                                        deviceName.startsWith("nvme") ||  // NVMe
                                        deviceName.startsWith("hd")) {    // IDE
                                        
                                        try {
                                            // Read device information
                                            String model = "";
                                            String vendor = "";
                                            String serial = "";
                                            String size = "";
                                            
                                            File modelFile = new File(device, "device/model");
                                            if (modelFile.exists()) {
                                                model = Files.readString(modelFile.toPath()).trim();
                                            }
                                            
                                            File vendorFile = new File(device, "device/vendor");
                                            if (vendorFile.exists()) {
                                                vendor = Files.readString(vendorFile.toPath()).trim();
                                            }
                                            
                                            File serialFile = new File(device, "device/serial");
                                            if (serialFile.exists()) {
                                                serial = Files.readString(serialFile.toPath()).trim();
                                            }
                                            
                                            File sizeFile = new File(device, "size");
                                            if (sizeFile.exists()) {
                                                size = Files.readString(sizeFile.toPath()).trim();
                                            }
                                            
                                            // Add device information
                                            hardwareData.append("  Physical Storage: ").append(deviceName).append("\n");
                                            if (!model.isEmpty()) hardwareData.append("    Model: ").append(model).append("\n");
                                            if (!vendor.isEmpty()) hardwareData.append("    Vendor: ").append(vendor).append("\n");
                                            if (!serial.isEmpty()) hardwareData.append("    Serial: ").append(serial).append("\n");
                                            if (!size.isEmpty()) {
                                                long sizeInBytes = Long.parseLong(size) * 512;
                                                hardwareData.append("    Size: ").append(formatBytes(sizeInBytes)).append("\n");
                                            }
                                            
                                            // Read removable status if available
                                            File removableFile = new File(device, "removable");
                                            if (removableFile.exists()) {
                                                String removable = Files.readString(removableFile.toPath()).trim();
                                                hardwareData.append("    Removable: ").append("1".equals(removable) ? "Yes" : "No").append("\n");
                                            }
                                            
                                        } catch (Exception e) {
                                            LoggingService.logDebug(MODULE_NAME, "Error reading device " + deviceName + ": " + e.getMessage());
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Use existing OSHI implementation
                        for (HWDiskStore disk : disks) {
                            hardwareData.append("  ").append(disk.getName()).append(": ").append(disk.getModel())
                                        .append(" (").append(formatBytes(disk.getSize())).append(")")
                                        .append(" Serial: ").append(disk.getSerial()).append("\n");
                        }
                    }
                } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Error collecting storage information", e);
                    hardwareData.append("Storage Information: Error collecting data\n");
                }
            });
            
            try {
                future.get(3, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                LoggingService.logError(MODULE_NAME, "Storage information collection timed out", e);
                hardwareData.append("Storage Information: Collection timed out\n");
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME, "Error in storage information collection", e);
                hardwareData.append("Storage Information: Error collecting data\n");
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error in storage information collection", e);
            hardwareData.append("Storage Information: Error collecting data\n");
        }
    }

    private void collectNetworkInfo(StringBuilder hardwareData, HardwareAbstractionLayer hal) {
        try {
            LoggingService.logDebug(MODULE_NAME, "Collecting network information");
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    hardwareData.append("\n=== Physical Network Interfaces ===\n");
                    
                    // Read network interfaces from /sys/class/net
                    File netDir = new File("/sys/class/net");
                    if (netDir.exists() && netDir.isDirectory()) {
                        File[] interfaces = netDir.listFiles();
                        if (interfaces != null) {
                            // Check if we're in a container using environment variable
                            boolean isContainer = "container".equals(System.getenv("IOFOG_DAEMON").toLowerCase());
                            LoggingService.logDebug(MODULE_NAME, "Running in container: " + isContainer);

                            for (File iface : interfaces) {
                                String ifaceName = iface.getName();
                                
                                // Skip interfaces based on environment
                                if (isContainer) {
                                    // In container, show all eth interfaces and skip virtual interfaces
                                    if (!ifaceName.startsWith("eth") || 
                                        ifaceName.startsWith("lo") || 
                                        ifaceName.startsWith("docker") || 
                                        ifaceName.startsWith("br-") ||
                                        ifaceName.startsWith("veth") ||
                                        ifaceName.startsWith("bond") ||
                                        ifaceName.startsWith("tun") ||
                                        ifaceName.startsWith("tap") ||
                                        ifaceName.startsWith("ip6") ||
                                        ifaceName.startsWith("sit") ||
                                        ifaceName.startsWith("gre") ||
                                        ifaceName.startsWith("erspan") ||
                                        ifaceName.startsWith("dummy")) {
                                        continue;
                                    }
                                } else {
                                    // In host, skip only virtual interfaces
                                    if (ifaceName.startsWith("lo") || 
                                        ifaceName.startsWith("docker") || 
                                        ifaceName.startsWith("br-") ||
                                        ifaceName.startsWith("veth") ||
                                        ifaceName.startsWith("bond") ||
                                        ifaceName.startsWith("tun") ||
                                        ifaceName.startsWith("tap") ||
                                        ifaceName.startsWith("ip6") ||
                                        ifaceName.startsWith("sit") ||
                                        ifaceName.startsWith("gre") ||
                                        ifaceName.startsWith("erspan") ||
                                        ifaceName.startsWith("dummy")) {
                                        continue;
                                    }
                                }

                                try {
                                    // Read MAC address first
                                    File addressFile = new File(iface, "address");
                                    if (addressFile.exists()) {
                                        String mac = Files.readString(addressFile.toPath()).trim();
                                        if (!mac.isEmpty() && !mac.equals("00:00:00:00:00:00")) {
                                            hardwareData.append("  Physical NIC: ").append(ifaceName).append("\n");
                                            hardwareData.append("    MAC: ").append(mac).append("\n");
                                            
                                            // Check carrier status (cable plugged in)
                                            File carrierFile = new File(iface, "carrier");
                                            if (carrierFile.exists()) {
                                                String carrier = Files.readString(carrierFile.toPath()).trim();
                                                hardwareData.append("    Cable Status: ").append("1".equals(carrier) ? "Connected" : "Disconnected").append("\n");
                                            }
                                            
                                            // Read speed if available
                                            File speedFile = new File(iface, "speed");
                                            if (speedFile.exists()) {
                                                try {
                                                    String speed = Files.readString(speedFile.toPath()).trim();
                                                    if (!speed.equals("-1")) {
                                                        hardwareData.append("    Speed: ").append(speed).append(" Mbps\n");
                                                    }
                                                } catch (Exception e) {
                                                    // Ignore speed reading errors
                                                }
                                            }
                                            
                                            // Read duplex if available
                                            File duplexFile = new File(iface, "duplex");
                                            if (duplexFile.exists()) {
                                                try {
                                                    String duplex = Files.readString(duplexFile.toPath()).trim();
                                                    if (!duplex.equals("unknown")) {
                                                        hardwareData.append("    Duplex: ").append(duplex).append("\n");
                                                    }
                                                } catch (Exception e) {
                                                    // Ignore duplex reading errors
                                                }
                                            }

                                            // Read device type if available
                                            File typeFile = new File(iface, "type");
                                            if (typeFile.exists()) {
                                                try {
                                                    String type = Files.readString(typeFile.toPath()).trim();
                                                    hardwareData.append("    Type: ").append(type).append("\n");
                                                } catch (Exception e) {
                                                    // Ignore type reading errors
                                                }
                                            }

                                            // Read device driver if available
                                            File driverFile = new File(iface, "device/driver");
                                            if (driverFile.exists()) {
                                                try {
                                                    String driver = driverFile.getName();
                                                    hardwareData.append("    Driver: ").append(driver).append("\n");
                                                } catch (Exception e) {
                                                    // Ignore driver reading errors
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    // Log but continue with next interface
                                    LoggingService.logDebug(MODULE_NAME, "Error reading interface " + ifaceName + ": " + e.getMessage());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Error collecting network information", e);
                    hardwareData.append("Network Information: Error collecting data\n");
                }
            });
            
            try {
                future.get(3, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                LoggingService.logError(MODULE_NAME, "Network information collection timed out", e);
                hardwareData.append("Network Information: Collection timed out\n");
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME, "Error in network information collection", e);
                hardwareData.append("Network Information: Error collecting data\n");
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error in network information collection", e);
            hardwareData.append("Network Information: Error collecting data\n");
        }
    }

    private void collectUsbInfo(StringBuilder hardwareData, HardwareAbstractionLayer hal) {
        try {
            LoggingService.logDebug(MODULE_NAME, "Collecting USB information");
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    hardwareData.append("\n=== USB Devices ===\n");
                    
                    // Check if we're in a container using environment variable
                    boolean isContainer = "container".equals(System.getenv("IOFOG_DAEMON").toLowerCase());
                    LoggingService.logDebug(MODULE_NAME, "Running in container: " + isContainer);

                    // Read USB devices from /sys/bus/usb/devices
                    File usbDir = new File("/sys/bus/usb/devices");
                    if (usbDir.exists() && usbDir.isDirectory()) {
                        File[] devices = usbDir.listFiles();
                        if (devices != null) {
                            // First, identify USB controllers
                            Map<String, String> controllers = new HashMap<>();
                            for (File device : devices) {
                                String deviceName = device.getName();
                                if (deviceName.startsWith("usb")) {
                                    try {
                                        // This is a USB controller
                                        File idVendor = new File(device, "idVendor");
                                        File idProduct = new File(device, "idProduct");
                                        if (idVendor.exists() && idProduct.exists()) {
                                            String vendorId = Files.readString(idVendor.toPath()).trim();
                                            String productId = Files.readString(idProduct.toPath()).trim();
                                            controllers.put(deviceName, vendorId + ":" + productId);
                                        }
                                    } catch (Exception e) {
                                        // Log but continue with next controller
                                        LoggingService.logDebug(MODULE_NAME, "Error reading USB controller " + deviceName + ": " + e.getMessage());
                                    }
                                }
                            }

                            // Then process all devices
                            for (File device : devices) {
                                String deviceName = device.getName();
                                // Skip the usb controllers themselves
                                if (deviceName.startsWith("usb")) {
                                    continue;
                                }

                                try {
                                    // Get the parent controller
                                    String parentController = deviceName.split("-")[0];
                                    String controllerInfo = controllers.get(parentController);

                                    // Read vendor and product IDs
                                    File idVendor = new File(device, "idVendor");
                                    File idProduct = new File(device, "idProduct");
                                    
                                    if (idVendor.exists() && idProduct.exists()) {
                                        String vendorId = Files.readString(idVendor.toPath()).trim();
                                        String productId = Files.readString(idProduct.toPath()).trim();
                                        
                                        // Read manufacturer and product if available
                                        String manufacturer = "";
                                        String product = "";
                                        
                                        File manufacturerFile = new File(device, "manufacturer");
                                        if (manufacturerFile.exists()) {
                                            manufacturer = Files.readString(manufacturerFile.toPath()).trim();
                                        }
                                        
                                        File productFile = new File(device, "product");
                                        if (productFile.exists()) {
                                            product = Files.readString(productFile.toPath()).trim();
                                        }

                                        // Check if this is a hub
                                        boolean isHub = false;
                                        File bDeviceClass = new File(device, "bDeviceClass");
                                        if (bDeviceClass.exists()) {
                                            String deviceClass = Files.readString(bDeviceClass.toPath()).trim();
                                            isHub = "09".equals(deviceClass); // 09 is the USB hub class
                                        }
                                        
                                        hardwareData.append("  USB Device: ").append(deviceName).append("\n");
                                        if (controllerInfo != null) {
                                            hardwareData.append("    Controller: ").append(controllerInfo).append("\n");
                                        }
                                        hardwareData.append("    Vendor ID: ").append(vendorId).append("\n");
                                        hardwareData.append("    Product ID: ").append(productId).append("\n");
                                        if (!manufacturer.isEmpty()) {
                                            hardwareData.append("    Manufacturer: ").append(manufacturer).append("\n");
                                        }
                                        if (!product.isEmpty()) {
                                            hardwareData.append("    Product: ").append(product).append("\n");
                                        }
                                        if (isHub) {
                                            hardwareData.append("    Type: USB Hub\n");
                                        }
                                        
                                        // Read serial number if available
                                        File serialFile = new File(device, "serial");
                                        if (serialFile.exists()) {
                                            String serial = Files.readString(serialFile.toPath()).trim();
                                            hardwareData.append("    Serial: ").append(serial).append("\n");
                                        }

                                        // Read speed if available
                                        File speedFile = new File(device, "speed");
                                        if (speedFile.exists()) {
                                            String speed = Files.readString(speedFile.toPath()).trim();
                                            hardwareData.append("    Speed: ").append(speed).append("\n");
                                        }

                                        // Read device class if available
                                        File classFile = new File(device, "bDeviceClass");
                                        if (classFile.exists()) {
                                            String deviceClass = Files.readString(classFile.toPath()).trim();
                                            hardwareData.append("    Class: ").append(deviceClass).append("\n");
                                        }

                                        // Read device subclass if available
                                        File subclassFile = new File(device, "bDeviceSubClass");
                                        if (subclassFile.exists()) {
                                            String deviceSubclass = Files.readString(subclassFile.toPath()).trim();
                                            hardwareData.append("    Subclass: ").append(deviceSubclass).append("\n");
                                        }

                                        // Read device protocol if available
                                        File protocolFile = new File(device, "bDeviceProtocol");
                                        if (protocolFile.exists()) {
                                            String deviceProtocol = Files.readString(protocolFile.toPath()).trim();
                                            hardwareData.append("    Protocol: ").append(deviceProtocol).append("\n");
                                        }
                                    }
                                } catch (Exception e) {
                                    // Log but continue with next device
                                    LoggingService.logDebug(MODULE_NAME, "Error reading USB device " + deviceName + ": " + e.getMessage());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Error collecting USB information", e);
                    hardwareData.append("USB Information: Error collecting data\n");
                }
            });
            
            try {
                future.get(3, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                LoggingService.logError(MODULE_NAME, "USB information collection timed out", e);
                hardwareData.append("USB Information: Collection timed out\n");
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME, "Error in USB information collection", e);
                hardwareData.append("USB Information: Error collecting data\n");
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error in USB information collection", e);
            hardwareData.append("USB Information: Error collecting data\n");
        }
    }

    private void collectOsInfo(StringBuilder hardwareData, SystemInfo systemInfo) {
        try {
            LoggingService.logDebug(MODULE_NAME, "Collecting OS information");
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    hardwareData.append("\n=== Operating System ===\n");
                    
                    // Check if we're in a container using environment variable
                    boolean isContainer = "container".equals(System.getenv("IOFOG_DAEMON").toLowerCase());
                    hardwareData.append("IoFog-Daemon: ").append(isContainer ? "CONTAINER" : "NATIVE").append("\n");
                    
                    OperatingSystem os = systemInfo.getOperatingSystem();
                    hardwareData.append("OS: ").append(os.toString()).append("\n");
                    hardwareData.append("Family: ").append(os.getFamily()).append("\n");
                    hardwareData.append("Manufacturer: ").append(os.getManufacturer()).append("\n");
                    hardwareData.append("Version: ").append(os.getVersionInfo().toString()).append("\n");
                    hardwareData.append("Architecture: ").append(System.getProperty("os.arch")).append("\n");
                    hardwareData.append("Kernel: ").append(System.getProperty("os.version")).append("\n");
                } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Error collecting OS information", e);
                    hardwareData.append("OS Information: Error collecting data\n");
                }
            });
            
            try {
                future.get(3, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                LoggingService.logError(MODULE_NAME, "OS information collection timed out", e);
                hardwareData.append("OS Information: Collection timed out\n");
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME, "Error in OS information collection", e);
                hardwareData.append("OS Information: Error collecting data\n");
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error in OS information collection", e);
            hardwareData.append("OS Information: Error collecting data\n");
        }
    }

    // private void collectPowerInfo(StringBuilder hardwareData, HardwareAbstractionLayer hal) {
    //     try {
    //         LoggingService.logDebug(MODULE_NAME, "Collecting power information");
    //         CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
    //             try {
    //                 hardwareData.append("\n=== Power Sources ===\n");
    //                 for (PowerSource ps : hal.getPowerSources()) {
    //                     hardwareData.append("Battery: ").append(ps.getName()).append("\n");
    //                     hardwareData.append("  Manufacturer: ").append(ps.getManufacturer()).append("\n");
    //                     hardwareData.append("  Chemistry: ").append(ps.getChemistry()).append("\n");
    //                     hardwareData.append("  Design Capacity: ").append(ps.getDesignCapacity()).append(" mWh\n");
    //                     hardwareData.append("  Max Capacity: ").append(ps.getMaxCapacity()).append(" mWh\n");
    //                 }
    //             } catch (Exception e) {
    //                 LoggingService.logError(MODULE_NAME, "Error collecting power information", e);
    //                 hardwareData.append("Power Information: Error collecting data\n");
    //             }
    //         });
            
    //         try {
    //             future.get(3, TimeUnit.SECONDS);
    //         } catch (TimeoutException e) {
    //             LoggingService.logError(MODULE_NAME, "Power information collection timed out", e);
    //             hardwareData.append("Power Information: Collection timed out\n");
    //         } catch (Exception e) {
    //             LoggingService.logError(MODULE_NAME, "Error in power information collection", e);
    //             hardwareData.append("Power Information: Error collecting data\n");
    //         }
    //     } catch (Exception e) {
    //         LoggingService.logError(MODULE_NAME, "Error in power information collection", e);
    //         hardwareData.append("Power Information: Error collecting data\n");
    //     }
    // }

    // private void collectSensorInfo(StringBuilder hardwareData, HardwareAbstractionLayer hal) {
    //     try {
    //         LoggingService.logDebug(MODULE_NAME, "Collecting sensor information");
    //         CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
    //             try {
    //                 hardwareData.append("\n=== Sensor Hardware ===\n");
    //                 Sensors sensors = hal.getSensors();
    //                 hardwareData.append("Temperature Sensors Available: ").append(sensors.getCpuTemperature() != 0 ? "Yes" : "No").append("\n");
    //                 hardwareData.append("Fan Sensors Count: ").append(sensors.getFanSpeeds().length).append("\n");
    //                 hardwareData.append("Voltage Sensors Available: ").append(sensors.getCpuVoltage() != 0 ? "Yes" : "No").append("\n");
    //             } catch (Exception e) {
    //                 LoggingService.logError(MODULE_NAME, "Error collecting sensor information", e);
    //                 hardwareData.append("Sensor Information: Error collecting data\n");
    //             }
    //         });
            
    //         try {
    //             future.get(3, TimeUnit.SECONDS);
    //         } catch (TimeoutException e) {
    //             LoggingService.logError(MODULE_NAME, "Sensor information collection timed out", e);
    //             hardwareData.append("Sensor Information: Collection timed out\n");
    //         } catch (Exception e) {
    //             LoggingService.logError(MODULE_NAME, "Error in sensor information collection", e);
    //             hardwareData.append("Sensor Information: Error collecting data\n");
    //         }
    //     } catch (Exception e) {
    //         LoggingService.logError(MODULE_NAME, "Error in sensor information collection", e);
    //         hardwareData.append("Sensor Information: Error collecting data\n");
    //     }
    // }

    private void collectPciInfo(StringBuilder hardwareData, HardwareAbstractionLayer hal) {
        try {
            LoggingService.logDebug(MODULE_NAME, "Collecting PCI device information");
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    hardwareData.append("\n=== PCI Devices ===\n");
                    
                    // Graphics cards
                    hardwareData.append("Graphics Cards:\n");
                    for (GraphicsCard gpu : hal.getGraphicsCards()) {
                        hardwareData.append("  ").append(gpu.getDeviceId()).append(" ").append(gpu.getName())
                                    .append(" (rev ").append(gpu.getVersionInfo()).append(")\n");
                        hardwareData.append("    Vendor: ").append(gpu.getVendor()).append("\n");
                        hardwareData.append("    VRAM: ").append(formatBytes(gpu.getVRam())).append("\n");
                    }
                    
                    // Sound cards
                    hardwareData.append("Sound Cards:\n");
                    for (SoundCard sound : hal.getSoundCards()) {
                        hardwareData.append("  ").append(sound.getCodec()).append(" ").append(sound.getName()).append("\n");
                        hardwareData.append("    Driver: ").append(sound.getDriverVersion()).append("\n");
                    }
                } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Error collecting PCI device information", e);
                    hardwareData.append("PCI Device Information: Error collecting data\n");
                }
            });
            
            try {
                future.get(3, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                LoggingService.logError(MODULE_NAME, "PCI device information collection timed out", e);
                hardwareData.append("PCI Device Information: Collection timed out\n");
            } catch (Exception e) {
                LoggingService.logError(MODULE_NAME, "Error in PCI device information collection", e);
                hardwareData.append("PCI Device Information: Error collecting data\n");
            }
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Error in PCI device information collection", e);
            hardwareData.append("PCI Device Information: Error collecting data\n");
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private String hashData(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            LoggingService.logError(MODULE_NAME, "Error hashing data", e);
            return null;
        }
    }

    private String signWithPrivateKey(String hash) {
        try {
            // Get and validate private key
            String base64Key = Configuration.getPrivateKey();
            if (base64Key == null || base64Key.isEmpty()) {
                LoggingService.logError(MODULE_NAME, "Private key is not configured", new Exception("Private key is not configured"));
                return null;
            }

            // Initialize signer if needed
            if (signer == null || keyPair == null) {
                try {
                    // Parse the base64-encoded JWK
                    byte[] keyBytes = Base64.getDecoder().decode(base64Key);
                    String jwkJson = new String(keyBytes);
                    // LoggingService.logDebug(MODULE_NAME, "Parsing JWK: " + jwkJson);
                    
                    // Parse and validate the JWK
                    keyPair = OctetKeyPair.parse(jwkJson);
                    if (!"OKP".equals(keyPair.getKeyType().getValue())) {
                        LoggingService.logError(MODULE_NAME, "Invalid key type", new Exception("Key must be OKP type"));
                        return null;
                    }
                    if (!"Ed25519".equals(keyPair.getCurve().getName())) {
                        LoggingService.logError(MODULE_NAME, "Invalid curve", new Exception("Key must use Ed25519 curve"));
                        return null;
                    }
                    
                    signer = new Ed25519Signer(keyPair);
                    LoggingService.logDebug(MODULE_NAME, "Successfully initialized Ed25519 signer");
                } catch (Exception e) {
                    LoggingService.logError(MODULE_NAME, "Failed to initialize signer: " + e.getMessage(), e);
                    return null;
                }
            }

            // Create JWT claims with only the hash value
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .claim("hash", hash)
                .build();

            // Create JWS header with just EdDSA algorithm
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.EdDSA)
                .build();

            // Create and sign JWT
            SignedJWT signedJWT = new SignedJWT(header, claims);
            signedJWT.sign(signer);

            String jwt = signedJWT.serialize();
            LoggingService.logDebug(MODULE_NAME, "Generated JWT with hardware hash");
            return jwt;
        } catch (Exception e) {
            LoggingService.logError(MODULE_NAME, "Failed to sign hardware signature: " + e.getMessage(), e);
            return null;
        }
    }
} 