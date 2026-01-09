package tranhuy105.evrptw.util;

/**
 * Simple logging utility for EVRPTW solver
 */
public class Logger {
    private static LogLevel level = LogLevel.INFO;

    public static void setLevel(LogLevel newLevel) {
        level = newLevel;
    }

    public static LogLevel getLevel() {
        return level;
    }

    public static boolean isDebugEnabled() {
        return level.getValue() <= LogLevel.DEBUG.getValue();
    }

    private static void log(LogLevel msgLevel, String message) {
        if (msgLevel.getValue() >= level.getValue()) {
            System.out.println("[" + msgLevel.name() + "] " + message);
        }
    }

    public static void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    public static void info(String message) {
        log(LogLevel.INFO, message);
    }

    public static void warning(String message) {
        log(LogLevel.WARNING, message);
    }

    public static void error(String message) {
        log(LogLevel.ERROR, message);
    }
}
