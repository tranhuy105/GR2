package tranhuy105.evrptw.model;

/**
 * Type of node in EVRPTW problem
 */
public enum NodeType {
    DEPOT('d'),
    CUSTOMER('c'),
    STATION('f');

    private final char code;

    NodeType(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    /**
     * Get NodeType from character code
     */
    public static NodeType fromChar(char c) {
        return switch (Character.toLowerCase(c)) {
            case 'd' -> DEPOT;
            case 'c' -> CUSTOMER;
            case 'f' -> STATION;
            default -> throw new IllegalArgumentException("Invalid node type: " + c);
        };
    }
}
