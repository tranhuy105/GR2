package tranhuy105.evrptw.util;

/**
 * Logging levels for the EVRPTW solver
 */
public enum LogLevel {
    DEBUG(0),
    INFO(1),
    WARNING(2),
    ERROR(3);

    private final int value;

    LogLevel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
