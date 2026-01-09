package com.tranhuy105.server.domain;

/**
 * Type of node in the routing problem
 */
public enum NodeType {
    DEPOT('D'),
    CUSTOMER('C'),
    STATION('S');

    private final char code;

    NodeType(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static NodeType fromChar(char c) {
        return switch (Character.toUpperCase(c)) {
            case 'D' -> DEPOT;
            case 'C' -> CUSTOMER;
            case 'S', 'F' -> STATION;  // F = fuel station in some formats
            default -> throw new IllegalArgumentException("Unknown node type: " + c);
        };
    }
}
