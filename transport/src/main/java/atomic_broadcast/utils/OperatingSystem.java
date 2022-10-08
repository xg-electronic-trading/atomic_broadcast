package atomic_broadcast.utils;

public enum OperatingSystem {
    MacOS,
    Windows,
    Linux;


    public static OperatingSystem from(String os) {
        if (os.contains("Mac")) {
            return MacOS;
        }

        if (os.contains("Win")) {
            return  Windows;
        }

        return Linux;
    }
}
