package org.eclipse.iofog.hardware_manager;

import oshi.hardware.*;
import oshi.hardware.platform.linux.*;
import oshi.software.os.linux.LinuxOSVersionInfoEx;

import javax.json.*;
import java.util.ArrayList;
import java.util.List;

public class HardwareJsonConverter {
    public static JsonObject processorToJson(LinuxCentralProcessor linuxCentralProcessor) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("name", linuxCentralProcessor.getName());
        builder.add("cpu64", linuxCentralProcessor.isCpu64bit());
        builder.add("family", linuxCentralProcessor.getFamily());
        builder.add("identifier", linuxCentralProcessor.getIdentifier());
        builder.add("model", linuxCentralProcessor.getModel());
        builder.add("processorId", linuxCentralProcessor.getProcessorID());
        builder.add("vendor", linuxCentralProcessor.getVendor());
        builder.add("stepping", linuxCentralProcessor.getStepping());
        return builder.build();
    }

    public static LinuxCentralProcessor processorFromJson(JsonObject obj) {
        LinuxCentralProcessor linuxCentralProcessor = new LinuxCentralProcessor();
        linuxCentralProcessor.setName(obj.getString("name"));
        linuxCentralProcessor.setCpu64(obj.getBoolean("cpu64"));
        linuxCentralProcessor.setFamily(obj.getString("family"));
        linuxCentralProcessor.setIdentifier(obj.getString("identifier"));
        linuxCentralProcessor.setModel(obj.getString("model"));
        linuxCentralProcessor.setProcessorID(obj.getString("processorId"));
        linuxCentralProcessor.setVendor(obj.getString("vendor"));
        linuxCentralProcessor.setStepping(obj.getString("stepping"));
        return linuxCentralProcessor;
    }

    private static JsonObject baseboardToJson(Baseboard baseboard) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("manufacturer", baseboard.getManufacturer());
        builder.add("model", baseboard.getModel());
        builder.add("version", baseboard.getVersion());
        builder.add("serialNumber", baseboard.getSerialNumber());
        return builder.build();
    }

    public static Baseboard baseboardFromJson(JsonObject obj) {
        return new Baseboard() {
            @Override
            public String getManufacturer() {
                return obj.getString("manufacturer");
            }

            @Override
            public String getModel() {
                return obj.getString("model");
            }

            @Override
            public String getVersion() {
                return obj.getString("version");
            }

            @Override
            public String getSerialNumber() {
                return obj.getString("serialNumber");
            }
        };
    }

    private static JsonObject firmwareToJson(Firmware firmware) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("manufacturer", firmware.getManufacturer());
        builder.add("name", firmware.getName());
        builder.add("description", firmware.getDescription());
        builder.add("version", firmware.getVersion());
        builder.add("releaseDate", firmware.getReleaseDate());
        return builder.build();
    }

    private static Firmware firmwareFromJson(JsonObject obj) {
        return new Firmware() {
            @Override
            public String getManufacturer() {
                return obj.getString("manufacturer");
            }

            @Override
            public String getName() {
                return obj.getString("name");
            }

            @Override
            public String getDescription() {
                return obj.getString("description");
            }

            @Override
            public String getVersion() {
                return obj.getString("version");
            }

            @Override
            public String getReleaseDate() {
                return obj.getString("releaseDate");
            }
        };
    }

    public static JsonObject computerSystemToJson(ComputerSystem computerSystem) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("manufacturer", computerSystem.getManufacturer());
        builder.add("model", computerSystem.getModel());
        builder.add("serialNumber", computerSystem.getSerialNumber());
        builder.add("firmware", firmwareToJson(computerSystem.getFirmware()));
        builder.add("baseboard", baseboardToJson(computerSystem.getBaseboard()));
        return builder.build();
    }

    public static ComputerSystem computerSystemFromJson(JsonObject obj) {
        return new ComputerSystem() {
            @Override
            public String getManufacturer() {
                return obj.getString("manufacturer");
            }

            @Override
            public String getModel() {
                return obj.getString("model");
            }

            @Override
            public String getSerialNumber() {
                return obj.getString("serialNumber");
            }

            @Override
            public Firmware getFirmware() {
                return firmwareFromJson(obj.getJsonObject("firmware"));
            }

            @Override
            public Baseboard getBaseboard() {
                return baseboardFromJson(obj.getJsonObject("baseboard"));
            }
        };
    }

    public static JsonArray hardwareDisksListToJson(List<HWDiskStore> hwDiskStores) {
        JsonArrayBuilder builder = Json.createArrayBuilder();

        for (HWDiskStore diskStore : hwDiskStores) {
            JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
            objectBuilder.add("model", diskStore.getModel());
            objectBuilder.add("name", diskStore.getName());
            objectBuilder.add("partitions", partitionsArrayToJson(diskStore.getPartitions()));
            objectBuilder.add("serial", diskStore.getSerial());
            objectBuilder.add("size", diskStore.getSize());
            builder.add(objectBuilder);
        }

        return builder.build();
    }

    public static List<HWDiskStore> hardwareDisksListFromJson(JsonArray array) {
        List<HWDiskStore> list = new ArrayList<>();

        for (JsonValue jsonValue : array) {
            JsonObject obj = (JsonObject) jsonValue;
            HWDiskStore store = new HWDiskStore();
            store.setModel(obj.getString("model"));
            store.setName(obj.getString("name"));
            store.setPartitions(partitionsArrayFromJson(obj.getJsonArray("partitions")));
            store.setSerial(obj.getString("serial"));
            store.setSize(obj.getJsonNumber("size").longValue());
            list.add(store);
        }

        return list;
    }

    private static HWPartition[] partitionsArrayFromJson(JsonArray partitions) {
        List<HWPartition> partitionsList = new ArrayList<>();

        for (JsonValue jsonValue : partitions) {
            JsonObject obj = (JsonObject) jsonValue;
            HWPartition partition = new HWPartition();
            partition.setIdentification(obj.getString("identification"));
            partition.setName(obj.getString("name"));
            partition.setType(obj.getString("type"));
            partition.setUuid(obj.getString("uuid"));
            partitionsList.add(partition);
        }

        HWPartition[] partitionsArray = new HWPartition[partitionsList.size()];

        return partitionsList.toArray(partitionsArray);
    }

    private static JsonArray partitionsArrayToJson(HWPartition[] partitions) {
        JsonArrayBuilder builder = Json.createArrayBuilder();

        for (HWPartition partition : partitions) {
            JsonObjectBuilder objBuilder = Json.createObjectBuilder();
            objBuilder.add("identification", partition.getIdentification());
            objBuilder.add("name", partition.getName());
            objBuilder.add("type", partition.getType());
            objBuilder.add("uuid", partition.getUuid());
            builder.add(objBuilder);
        }

        return builder.build();
    }

    public static JsonArray networkInterfacesToJson(List<NetworkIF> networkInterfaces) {
        JsonArrayBuilder builder = Json.createArrayBuilder();

        for (NetworkIF networkInterface : networkInterfaces) {
            JsonObjectBuilder objBuilder = Json.createObjectBuilder();
            objBuilder.add("displayName", networkInterface.getDisplayName());
            objBuilder.add("macAddress", networkInterface.getMacaddr());
            objBuilder.add("mtu", networkInterface.getMTU());
            builder.add(objBuilder);
        }

        return builder.build();
    }

    public static List<NetworkInterfaceModel> networkInterfacesFromJson(JsonArray networkInterfaces) {
        List<NetworkInterfaceModel> networkInterfacesList = new ArrayList<>();

        for (JsonValue jsonValue : networkInterfaces) {
            JsonObject obj = (JsonObject) jsonValue;
            NetworkInterfaceModel networkInterface = new NetworkInterfaceModel();
            networkInterface.setDisplayName(obj.getString("displayName"));
            networkInterface.setMacAddress(obj.getString("macAddress"));
            networkInterface.setMtu(obj.getInt("mtu"));
            networkInterfacesList.add(networkInterface);
        }

        return networkInterfacesList;
    }

    public static JsonObject linuxOsVersionToJson(LinuxOSVersionInfoEx linuxOSVersionInfoEx) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("buildNumber", linuxOSVersionInfoEx.getBuildNumber());
        builder.add("codeName", linuxOSVersionInfoEx.getCodeName());
        builder.add("version", linuxOSVersionInfoEx.getVersion());
        return builder.build();
    }

    public static LinuxOSVersionInfoEx linuxOsVersionFromJson(JsonObject obj) {
        LinuxOSVersionInfoEx linuxOsVersion = new LinuxOSVersionInfoEx();
        linuxOsVersion.setBuildNumber(obj.getString("buildNumber"));
        linuxOsVersion.setCodeName(obj.getString("codeName"));
        linuxOsVersion.setVersion(obj.getString("version"));
        return linuxOsVersion;
    }

    public static JsonArray displayListToJson(List<LinuxDisplay> displays) {
        JsonArrayBuilder builder = Json.createArrayBuilder();

        for (LinuxDisplay display : displays) {
            JsonObjectBuilder objBuilder = Json.createObjectBuilder();
            objBuilder.add("edid", new String(display.getEdid()));
            builder.add(objBuilder);
        }

        return builder.build();
    }

    public static List<LinuxDisplay> displayListFromJson(JsonArray array) {
        List<LinuxDisplay> displays = new ArrayList<>();

        for (JsonValue jsonValue : array) {
            JsonObject obj = (JsonObject) jsonValue;
            LinuxDisplay display = new LinuxDisplay(obj.getString("edid").getBytes());
            displays.add(display);
        }

        return displays;
    }

    public static JsonArray powerSourcesToJson(List<LinuxPowerSource> powerSources) {
        JsonArrayBuilder builder = Json.createArrayBuilder();

        for (LinuxPowerSource powerSource : powerSources) {
            JsonObjectBuilder objBuilder = Json.createObjectBuilder();
            objBuilder.add("name", powerSource.getName());
            objBuilder.add("remainingCapacity", powerSource.getRemainingCapacity());
            objBuilder.add("timeRemaining", powerSource.getTimeRemaining());
            builder.add(objBuilder);
        }

        return builder.build();
    }

    public static List<LinuxPowerSource> powerSourcesFromJson(JsonArray array) {
        List<LinuxPowerSource> powerSources = new ArrayList<>();

        for (JsonValue jsonValue : array) {
            JsonObject obj = (JsonObject) jsonValue;
            LinuxPowerSource powerSource = new LinuxPowerSource(obj.getString("name"),
                    obj.getJsonNumber("remainingCapacity").doubleValue(),
                    obj.getJsonNumber("timeRemaining").doubleValue());
            powerSources.add(powerSource);
        }

        return powerSources;
    }

    public static JsonArray soundCardsToJson(List<LinuxSoundCard> soundCards) {
        JsonArrayBuilder builder = Json.createArrayBuilder();

        for (LinuxSoundCard soundCard : soundCards) {
            JsonObjectBuilder objBuilder = Json.createObjectBuilder();
            objBuilder.add("kernelVersion", soundCard.getDriverVersion());
            objBuilder.add("name", soundCard.getName());
            objBuilder.add("codec", soundCard.getCodec());
            builder.add(objBuilder);
        }

        return builder.build();
    }

    public static List<LinuxSoundCard> soundCardsFromJson(JsonArray array) {
        List<LinuxSoundCard> linuxSoundCards = new ArrayList<>();

        for (JsonValue jsonValue : array) {
            JsonObject obj = (JsonObject) jsonValue;
            LinuxSoundCard soundCard = new LinuxSoundCard(obj.getString("kernelVersion"),
                    obj.getString("name"), obj.getString("codec"));
            linuxSoundCards.add(soundCard);
        }

        return linuxSoundCards;
    }

    public static JsonArray usbDevicesToJson(List<LinuxUsbDevice> usbDevices) {
        JsonArrayBuilder builder = Json.createArrayBuilder();

        for (LinuxUsbDevice usbDevice : usbDevices) {
            JsonObjectBuilder objBuilder = Json.createObjectBuilder();
            objBuilder.add("name", usbDevice.getName());
            objBuilder.add("vendor", usbDevice.getVendor());
            objBuilder.add("vendorId", usbDevice.getVendorId());
            objBuilder.add("productId", usbDevice.getProductId());
            objBuilder.add("serialNumber", usbDevice.getSerialNumber());
            objBuilder.add("connectedDevices", connectedUsbDevicesArrayToJson(usbDevice.getConnectedDevices()));
            builder.add(objBuilder);
        }

        return builder.build();
    }

    public static List<LinuxUsbDevice> usbDevicesFromJson(JsonArray array) {
        List<LinuxUsbDevice> usbDevices = new ArrayList<>();

        for (JsonValue jsonValue : array) {
            JsonObject obj = (JsonObject) jsonValue;
            LinuxUsbDevice usbDevice = new LinuxUsbDevice(obj.getString("name"),
                    obj.getString("vendor"), obj.getString("vendorId"),
                    obj.getString("productId"), obj.getString("serialNumber"),
                    connectedUsbDevicesArrayFromJson(obj.getJsonArray("connectedDevices")));
            usbDevices.add(usbDevice);
        }

        return usbDevices;
    }

    private static UsbDevice[] connectedUsbDevicesArrayFromJson(JsonArray array) {
        List<UsbDevice> usbDevices = new ArrayList<>();

        for (JsonValue jsonValue : array) {
            JsonObject obj = (JsonObject) jsonValue;
            LinuxUsbDevice usbDevice = new LinuxUsbDevice(obj.getString("name"),
                    obj.getString("vendor"), obj.getString("vendorId"),
                    obj.getString("productId"), obj.getString("serialNumber"),
                    connectedUsbDevicesArrayFromJson(obj.getJsonArray("connectedDevices")));
            usbDevices.add(usbDevice);
        }

        UsbDevice[] connectedDevicesArray = new UsbDevice[usbDevices.size()];

        return usbDevices.toArray(connectedDevicesArray);
    }

    private static JsonArray connectedUsbDevicesArrayToJson(UsbDevice[] connectedDevices) {
        JsonArrayBuilder builder = Json.createArrayBuilder();

        for (UsbDevice usbDevice : connectedDevices) {
            JsonObjectBuilder objBuilder = Json.createObjectBuilder();
            objBuilder.add("name", usbDevice.getName());
            objBuilder.add("vendor", usbDevice.getVendor());
            objBuilder.add("vendorId", usbDevice.getVendorId());
            objBuilder.add("productId", usbDevice.getProductId());
            objBuilder.add("serialNumber", usbDevice.getSerialNumber());
            objBuilder.add("connectedDevices", connectedUsbDevicesArrayToJson(usbDevice.getConnectedDevices()));
            builder.add(objBuilder);
        }

        return builder.build();
    }

}
