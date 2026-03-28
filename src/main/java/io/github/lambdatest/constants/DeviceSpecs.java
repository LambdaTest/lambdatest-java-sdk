package io.github.lambdatest.constants;

import java.util.HashMap;
import java.util.Map;

public class DeviceSpecs {
    
    public static class DeviceSpec {
        private final String os;
        private final int width;
        private final int height;
        
        public DeviceSpec(String os, int width, int height) {
            this.os = os;
            this.width = width;
            this.height = height;
        }
        
        public String getOs() { return os; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
    }
    
    private static final Map<String, DeviceSpec> DEVICE_SPECS = new HashMap<>();
    
    static {
        DEVICE_SPECS.put("Blackberry KEY2 LE", new DeviceSpec("android", 412, 618));
        DEVICE_SPECS.put("Galaxy A12", new DeviceSpec("android", 360, 800));
        DEVICE_SPECS.put("Galaxy A21s", new DeviceSpec("android", 412, 915));
        DEVICE_SPECS.put("Galaxy A22", new DeviceSpec("android", 358, 857));
        DEVICE_SPECS.put("Galaxy A31", new DeviceSpec("android", 412, 915));
        DEVICE_SPECS.put("Galaxy A32", new DeviceSpec("android", 412, 915));
        DEVICE_SPECS.put("Galaxy A51", new DeviceSpec("android", 412, 915));
        DEVICE_SPECS.put("Galaxy A7", new DeviceSpec("android", 412, 846));
        DEVICE_SPECS.put("Galaxy A70", new DeviceSpec("android", 412, 915));
        DEVICE_SPECS.put("Galaxy A8", new DeviceSpec("android", 360, 740));
        DEVICE_SPECS.put("Galaxy A8 Plus", new DeviceSpec("android", 412, 846));
        DEVICE_SPECS.put("Galaxy J7 Prime", new DeviceSpec("android", 360, 640));
        DEVICE_SPECS.put("Galaxy M12", new DeviceSpec("android", 412, 915));
        DEVICE_SPECS.put("Galaxy M31", new DeviceSpec("android", 412, 892));
        DEVICE_SPECS.put("Galaxy Note10", new DeviceSpec("android", 412, 869));
        DEVICE_SPECS.put("Galaxy Note10 Plus", new DeviceSpec("android", 412, 869));
        DEVICE_SPECS.put("Galaxy Note20", new DeviceSpec("android", 412, 915));
        DEVICE_SPECS.put("Galaxy Note20 Ultra", new DeviceSpec("android", 412, 869));
        DEVICE_SPECS.put("Galaxy S10", new DeviceSpec("android", 360, 760));
        DEVICE_SPECS.put("Galaxy S10 Plus", new DeviceSpec("android", 412, 869));
        DEVICE_SPECS.put("Galaxy S10e", new DeviceSpec("android", 412, 740));
        DEVICE_SPECS.put("Galaxy S20", new DeviceSpec("android", 360, 800));
        DEVICE_SPECS.put("Galaxy S20 FE", new DeviceSpec("android", 412, 914));
        DEVICE_SPECS.put("Galaxy S20 Ultra", new DeviceSpec("android", 412, 915));
        DEVICE_SPECS.put("Galaxy S20 Plus", new DeviceSpec("android", 384, 854));
        DEVICE_SPECS.put("Galaxy S21", new DeviceSpec("android", 360, 800));
        DEVICE_SPECS.put("Galaxy S21 FE", new DeviceSpec("android", 360, 780));
        DEVICE_SPECS.put("Galaxy S21 Ultra", new DeviceSpec("android", 384, 854));
        DEVICE_SPECS.put("Galaxy S21 Plus", new DeviceSpec("android", 360, 800));
        DEVICE_SPECS.put("Galaxy S22", new DeviceSpec("android", 360, 780));
        DEVICE_SPECS.put("Galaxy S22 Ultra", new DeviceSpec("android", 384, 854));
        DEVICE_SPECS.put("Galaxy S23", new DeviceSpec("android", 360, 645));
        DEVICE_SPECS.put("Galaxy S23 Plus", new DeviceSpec("android", 360, 648));
        DEVICE_SPECS.put("Galaxy S23 Ultra", new DeviceSpec("android", 384, 689));
        DEVICE_SPECS.put("Galaxy S24", new DeviceSpec("android", 360, 780));
        DEVICE_SPECS.put("Galaxy S24 Plus", new DeviceSpec("android", 384, 832));
        DEVICE_SPECS.put("Galaxy S24 Ultra", new DeviceSpec("android", 384, 832));
        DEVICE_SPECS.put("Galaxy S7", new DeviceSpec("android", 360, 640));
        DEVICE_SPECS.put("Galaxy S7 Edge", new DeviceSpec("android", 360, 640));
        DEVICE_SPECS.put("Galaxy S8", new DeviceSpec("android", 360, 740));
        DEVICE_SPECS.put("Galaxy S8 Plus", new DeviceSpec("android", 360, 740));
        DEVICE_SPECS.put("Galaxy S9", new DeviceSpec("android", 360, 740));
        DEVICE_SPECS.put("Galaxy S9 Plus", new DeviceSpec("android", 360, 740));
        DEVICE_SPECS.put("Galaxy Tab A7 Lite", new DeviceSpec("android", 534, 894));
        DEVICE_SPECS.put("Galaxy Tab A8", new DeviceSpec("android", 800, 1280));
        DEVICE_SPECS.put("Galaxy Tab S3", new DeviceSpec("android", 1024, 768));
        DEVICE_SPECS.put("Galaxy Tab S4", new DeviceSpec("android", 712, 1138));
        DEVICE_SPECS.put("Galaxy Tab S7", new DeviceSpec("android", 800, 1192));
        DEVICE_SPECS.put("Galaxy Tab S8", new DeviceSpec("android", 753, 1205));
        DEVICE_SPECS.put("Galaxy Tab S8 Plus", new DeviceSpec("android", 825, 1318));
        DEVICE_SPECS.put("Huawei Mate 20 Pro", new DeviceSpec("android", 360, 780));
        DEVICE_SPECS.put("Huawei P20 Pro", new DeviceSpec("android", 360, 747));
        DEVICE_SPECS.put("Huawei P30", new DeviceSpec("android", 360, 780));
        DEVICE_SPECS.put("Huawei P30 Pro", new DeviceSpec("android", 360, 780));
        DEVICE_SPECS.put("Microsoft Surface Duo", new DeviceSpec("android", 1114, 705));
        DEVICE_SPECS.put("Moto G7 Play", new DeviceSpec("android", 360, 760));
        DEVICE_SPECS.put("Moto G9 Play", new DeviceSpec("android", 393, 786));
        DEVICE_SPECS.put("Moto G Stylus (2022)", new DeviceSpec("android", 432, 984));
        DEVICE_SPECS.put("Nexus 5", new DeviceSpec("android", 360, 640));
        DEVICE_SPECS.put("Nexus 5X", new DeviceSpec("android", 412, 732));
        DEVICE_SPECS.put("Nokia 5", new DeviceSpec("android", 360, 640));
        DEVICE_SPECS.put("Nothing Phone (1)", new DeviceSpec("android", 412, 915));
        DEVICE_SPECS.put("OnePlus 10 Pro", new DeviceSpec("android", 412, 919));
        DEVICE_SPECS.put("OnePlus 11", new DeviceSpec("android", 360, 804));
        DEVICE_SPECS.put("OnePlus 6", new DeviceSpec("android", 412, 869));
        DEVICE_SPECS.put("OnePlus 6T", new DeviceSpec("android", 412, 892));
        DEVICE_SPECS.put("OnePlus 7", new DeviceSpec("android", 412, 892));
        DEVICE_SPECS.put("OnePlus 7T", new DeviceSpec("android", 412, 914));
        DEVICE_SPECS.put("OnePlus 8", new DeviceSpec("android", 412, 915));
        DEVICE_SPECS.put("OnePlus 9", new DeviceSpec("android", 411, 915));
        DEVICE_SPECS.put("OnePlus 9 Pro", new DeviceSpec("android", 412, 919));
        DEVICE_SPECS.put("OnePlus Nord", new DeviceSpec("android", 412, 914));
        DEVICE_SPECS.put("OnePlus Nord 2", new DeviceSpec("android", 412, 915));
        DEVICE_SPECS.put("OnePlus Nord CE", new DeviceSpec("android", 412, 915));
        DEVICE_SPECS.put("Oppo A12", new DeviceSpec("android", 360, 760));
        DEVICE_SPECS.put("Oppo A15", new DeviceSpec("android", 360, 800));
        DEVICE_SPECS.put("Oppo A54", new DeviceSpec("android", 360, 800));
        DEVICE_SPECS.put("Oppo A5s", new DeviceSpec("android", 360, 760));
        DEVICE_SPECS.put("Oppo F17", new DeviceSpec("android", 360, 800));
        DEVICE_SPECS.put("Oppo K10", new DeviceSpec("android", 360, 804));
        DEVICE_SPECS.put("Pixel 3", new DeviceSpec("android", 412, 823));
        DEVICE_SPECS.put("Pixel 3 XL", new DeviceSpec("android", 412, 846));
        DEVICE_SPECS.put("Pixel 3a", new DeviceSpec("android", 412, 823));
        DEVICE_SPECS.put("Pixel 4", new DeviceSpec("android", 392, 830));
        DEVICE_SPECS.put("Pixel 4 XL", new DeviceSpec("android", 412, 823));
        DEVICE_SPECS.put("Pixel 4a", new DeviceSpec("android", 393, 851));
        DEVICE_SPECS.put("Pixel 5", new DeviceSpec("android", 393, 851));
        DEVICE_SPECS.put("Pixel 6", new DeviceSpec("android", 393, 786));
        DEVICE_SPECS.put("Pixel 6 Pro", new DeviceSpec("android", 412, 892));
        DEVICE_SPECS.put("Pixel 7", new DeviceSpec("android", 412, 915));
        DEVICE_SPECS.put("Pixel 7 Pro", new DeviceSpec("android", 412, 892));
        DEVICE_SPECS.put("Pixel 8", new DeviceSpec("android", 412, 915));
        DEVICE_SPECS.put("Pixel 8 Pro", new DeviceSpec("android", 448, 998));
        DEVICE_SPECS.put("Poco M2 Pro", new DeviceSpec("android", 393, 873));
        DEVICE_SPECS.put("POCO X3 Pro", new DeviceSpec("android", 393, 873));
        DEVICE_SPECS.put("Realme 5i", new DeviceSpec("android", 360, 800));
        DEVICE_SPECS.put("Realme 7i", new DeviceSpec("android", 360, 800));
        DEVICE_SPECS.put("Realme 8i", new DeviceSpec("android", 360, 804));
        DEVICE_SPECS.put("Realme C21Y", new DeviceSpec("android", 360, 800));
        DEVICE_SPECS.put("Realme C21", new DeviceSpec("android", 360, 800));
        DEVICE_SPECS.put("Realme GT2 Pro", new DeviceSpec("android", 360, 804));
        DEVICE_SPECS.put("Redmi 8", new DeviceSpec("android", 360, 760));
        DEVICE_SPECS.put("Redmi 9", new DeviceSpec("android", 360, 800));
        DEVICE_SPECS.put("Redmi 9C", new DeviceSpec("android", 360, 800));
        DEVICE_SPECS.put("Redmi Note 10 Pro", new DeviceSpec("android", 393, 873));
        DEVICE_SPECS.put("Redmi Note 8", new DeviceSpec("android", 393, 851));
        DEVICE_SPECS.put("Redmi Note 8 Pro", new DeviceSpec("android", 393, 851));
        DEVICE_SPECS.put("Redmi Note 9", new DeviceSpec("android", 393, 851));
        DEVICE_SPECS.put("Redmi Note 9 Pro Max", new DeviceSpec("android", 393, 873));
        DEVICE_SPECS.put("Redmi Y2", new DeviceSpec("android", 360, 720));
        DEVICE_SPECS.put("Tecno Spark 7", new DeviceSpec("android", 360, 800));
        DEVICE_SPECS.put("Vivo Y22", new DeviceSpec("android", 385, 860));
        DEVICE_SPECS.put("Vivo T1", new DeviceSpec("android", 393, 873));
        DEVICE_SPECS.put("Vivo V7", new DeviceSpec("android", 360, 720));
        DEVICE_SPECS.put("Vivo Y11", new DeviceSpec("android", 360, 722));
        DEVICE_SPECS.put("Vivo Y12", new DeviceSpec("android", 360, 722));
        DEVICE_SPECS.put("Vivo Y20g", new DeviceSpec("android", 385, 854));
        DEVICE_SPECS.put("Vivo Y50", new DeviceSpec("android", 393, 786));
        DEVICE_SPECS.put("Xiaomi 12 Pro", new DeviceSpec("android", 412, 915));
        DEVICE_SPECS.put("Xperia Z5", new DeviceSpec("android", 360, 640));
        DEVICE_SPECS.put("Xperia Z5 Dual", new DeviceSpec("android", 360, 640));
        DEVICE_SPECS.put("Zenfone 6", new DeviceSpec("android", 412, 892));
        DEVICE_SPECS.put("iPad 10.2 (2019)", new DeviceSpec("ios", 810, 1080));
        DEVICE_SPECS.put("iPad 10.2 (2020)", new DeviceSpec("ios", 834, 1194));
        DEVICE_SPECS.put("iPad 10.2 (2021)", new DeviceSpec("ios", 810, 1080));
        DEVICE_SPECS.put("iPad 9.7 (2017)", new DeviceSpec("ios", 768, 1024));
        DEVICE_SPECS.put("iPad Air (2019)", new DeviceSpec("ios", 834, 1112));
        DEVICE_SPECS.put("iPad Air (2020)", new DeviceSpec("ios", 820, 1180));
        DEVICE_SPECS.put("iPad Air (2022)", new DeviceSpec("ios", 820, 1180));
        DEVICE_SPECS.put("iPad mini (2019)", new DeviceSpec("ios", 768, 1024));
        DEVICE_SPECS.put("iPad mini (2021)", new DeviceSpec("ios", 744, 1133));
        DEVICE_SPECS.put("iPad Pro 11 (2021)", new DeviceSpec("ios", 834, 1194));
        DEVICE_SPECS.put("iPad Pro 11 (2022)", new DeviceSpec("ios", 834, 1194));
        DEVICE_SPECS.put("iPad Pro 12.9 (2018)", new DeviceSpec("ios", 1024, 1366));
        DEVICE_SPECS.put("iPad Pro 12.9 (2020)", new DeviceSpec("ios", 1024, 1366));
        DEVICE_SPECS.put("iPad Pro 12.9 (2021)", new DeviceSpec("ios", 1024, 1366));
        DEVICE_SPECS.put("iPad Pro 12.9 (2022)", new DeviceSpec("ios", 1024, 1366));
        DEVICE_SPECS.put("iPhone 11", new DeviceSpec("ios", 375, 812));
        DEVICE_SPECS.put("iPhone 11 Pro", new DeviceSpec("ios", 375, 812));
        DEVICE_SPECS.put("iPhone 11 Pro Max", new DeviceSpec("ios", 414, 896));
        DEVICE_SPECS.put("iPhone 12", new DeviceSpec("ios", 390, 844));
        DEVICE_SPECS.put("iPhone 12 Mini", new DeviceSpec("ios", 375, 812));
        DEVICE_SPECS.put("iPhone 12 Pro", new DeviceSpec("ios", 390, 844));
        DEVICE_SPECS.put("iPhone 12 Pro Max", new DeviceSpec("ios", 428, 926));
        DEVICE_SPECS.put("iPhone 13", new DeviceSpec("ios", 390, 844));
        DEVICE_SPECS.put("iPhone 13 Mini", new DeviceSpec("ios", 390, 844));
        DEVICE_SPECS.put("iPhone 13 Pro", new DeviceSpec("ios", 390, 844));
        DEVICE_SPECS.put("iPhone 13 Pro Max", new DeviceSpec("ios", 428, 926));
        DEVICE_SPECS.put("iPhone 14", new DeviceSpec("ios", 390, 844));
        DEVICE_SPECS.put("iPhone 14 Plus", new DeviceSpec("ios", 428, 926));
        DEVICE_SPECS.put("iPhone 14 Pro", new DeviceSpec("ios", 390, 844));
        DEVICE_SPECS.put("iPhone 14 Pro Max", new DeviceSpec("ios", 428, 928));
        DEVICE_SPECS.put("iPhone 15", new DeviceSpec("ios", 393, 852));
        DEVICE_SPECS.put("iPhone 15 Plus", new DeviceSpec("ios", 430, 932));
        DEVICE_SPECS.put("iPhone 15 Pro", new DeviceSpec("ios", 393, 852));
        DEVICE_SPECS.put("iPhone 15 Pro Max", new DeviceSpec("ios", 430, 932));
        DEVICE_SPECS.put("iPhone 6", new DeviceSpec("ios", 375, 667));
        DEVICE_SPECS.put("iPhone 6s", new DeviceSpec("ios", 375, 667));
        DEVICE_SPECS.put("iPhone 6s Plus", new DeviceSpec("ios", 414, 736));
        DEVICE_SPECS.put("iPhone 7", new DeviceSpec("ios", 375, 667));
        DEVICE_SPECS.put("iPhone 7 Plus", new DeviceSpec("ios", 414, 736));
        DEVICE_SPECS.put("iPhone 8", new DeviceSpec("ios", 375, 667));
        DEVICE_SPECS.put("iPhone 8 Plus", new DeviceSpec("ios", 414, 736));
        DEVICE_SPECS.put("iPhone SE (2016)", new DeviceSpec("ios", 320, 568));
        DEVICE_SPECS.put("iPhone SE (2020)", new DeviceSpec("ios", 375, 667));
        DEVICE_SPECS.put("iPhone SE (2022)", new DeviceSpec("ios", 375, 667));
        DEVICE_SPECS.put("iPhone X", new DeviceSpec("ios", 375, 812));
        DEVICE_SPECS.put("iPhone XR", new DeviceSpec("ios", 414, 896));
        DEVICE_SPECS.put("iPhone XS", new DeviceSpec("ios", 375, 812));
        DEVICE_SPECS.put("iPhone XS Max", new DeviceSpec("ios", 414, 896));
        DEVICE_SPECS.put("Galaxy A10s", new DeviceSpec("android", 360, 640));
        DEVICE_SPECS.put("Galaxy A11", new DeviceSpec("android", 412, 732));
        DEVICE_SPECS.put("Galaxy A13", new DeviceSpec("android", 412, 732));
        DEVICE_SPECS.put("Galaxy A52s 5G", new DeviceSpec("android", 384, 718));
        DEVICE_SPECS.put("Galaxy A53 5G", new DeviceSpec("android", 412, 915));
        DEVICE_SPECS.put("Galaxy Tab A 10.1 (2019)", new DeviceSpec("android", 800, 1280));
        DEVICE_SPECS.put("Galaxy Tab S9", new DeviceSpec("android", 753, 1069));
        DEVICE_SPECS.put("Honor X9a 5G", new DeviceSpec("android", 360, 678));
        DEVICE_SPECS.put("Huawei P30 Lite", new DeviceSpec("android", 360, 647));
        DEVICE_SPECS.put("Huawei P50 Pro", new DeviceSpec("android", 412, 915));
        DEVICE_SPECS.put("iPad Pro 13 (2024)", new DeviceSpec("ios", 1032, 1376));
        DEVICE_SPECS.put("iPad Pro 11 (2024)", new DeviceSpec("ios", 834, 1210));
        DEVICE_SPECS.put("iPad Air 13 (2024)", new DeviceSpec("ios", 1024, 1366));
        DEVICE_SPECS.put("iPad Air 11 (2024)", new DeviceSpec("ios", 820, 1180));
        DEVICE_SPECS.put("iPad 10.9 (2022)", new DeviceSpec("ios", 820, 1180));
        DEVICE_SPECS.put("iPhone 16", new DeviceSpec("ios", 393, 852));
        DEVICE_SPECS.put("iPhone 16 Plus", new DeviceSpec("ios", 430, 932));
        DEVICE_SPECS.put("iPhone 16 Pro", new DeviceSpec("ios", 402, 874));
        DEVICE_SPECS.put("iPhone 16 Pro Max", new DeviceSpec("ios", 440, 956));
        DEVICE_SPECS.put("Motorola Edge 40", new DeviceSpec("android", 412, 915));
        DEVICE_SPECS.put("Motorola Edge 30", new DeviceSpec("android", 432, 814));
        DEVICE_SPECS.put("Moto G22", new DeviceSpec("android", 412, 767));
        DEVICE_SPECS.put("Moto G54 5G", new DeviceSpec("android", 432, 810));
        DEVICE_SPECS.put("Moto G71 5G", new DeviceSpec("android", 412, 732));
        DEVICE_SPECS.put("Pixel Tablet", new DeviceSpec("android", 800, 1100));
        DEVICE_SPECS.put("Pixel 6a", new DeviceSpec("android", 412, 766));
        DEVICE_SPECS.put("Pixel 7a", new DeviceSpec("android", 412, 766));
        DEVICE_SPECS.put("Pixel 9", new DeviceSpec("android", 412, 924));
        DEVICE_SPECS.put("Pixel 9 Pro", new DeviceSpec("android", 412, 915));
        DEVICE_SPECS.put("Pixel 9 Pro XL", new DeviceSpec("android", 448, 998));
        DEVICE_SPECS.put("Redmi 9A", new DeviceSpec("android", 360, 800));
        DEVICE_SPECS.put("Redmi Note 13 Pro", new DeviceSpec("android", 412, 869));
        DEVICE_SPECS.put("Aquos Sense 5G", new DeviceSpec("android", 393, 731));
        DEVICE_SPECS.put("Xperia 10 IV", new DeviceSpec("android", 412, 832));
        DEVICE_SPECS.put("Honeywell CT40", new DeviceSpec("android", 360, 512));
    }
    
    public static DeviceSpec getDeviceSpec(String deviceName) {
        return DEVICE_SPECS.get(deviceName);
    }
    
    public static boolean isDeviceSupported(String deviceName) {
        return DEVICE_SPECS.containsKey(deviceName);
    }
    
    public static Map<String, DeviceSpec> getAllDeviceSpecs() {
        return new HashMap<>(DEVICE_SPECS);
    }
}
