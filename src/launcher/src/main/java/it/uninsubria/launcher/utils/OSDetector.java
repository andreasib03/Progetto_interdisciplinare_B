package it.uninsubria.launcher.utils;

public class OSDetector {

    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

    public static OperatingSystem detectOS() {
        if (OS_NAME.contains("win")) {
            return OperatingSystem.WINDOWS;
        } else if (OS_NAME.contains("mac")) {
            return OperatingSystem.MACOS;
        } else if (OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.contains("aix")) {
            return OperatingSystem.LINUX;
        } else {
            return OperatingSystem.UNKNOWN;
        }
    }

    public static String getOSName() {
        return System.getProperty("os.name");
    }

    public static String getOSVersion() {
        return System.getProperty("os.version");
    }

    public static String getOSArch() {
        return System.getProperty("os.arch");
    }

    public static boolean isWindows() {
        return detectOS() == OperatingSystem.WINDOWS;
    }

    public static boolean isMacOS() {
        return detectOS() == OperatingSystem.MACOS;
    }

    public static boolean isLinux() {
        return detectOS() == OperatingSystem.LINUX;
    }
}
