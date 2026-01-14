package com.tranhuy105.server.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a node in the routing problem (depot, customer, or station)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Node {
    private int id;
    private String stringId;
    private NodeType type;
    private double x;
    private double y;
    private double demand;
    private double readyTime;
    private double dueTime;
    private double serviceTime;

    /**
     * Factory method for creating from file format
     */
    public static Node fromFileFormat(String stringId, char typeChar, 
            double x, double y, double demand, 
            double readyTime, double dueTime, double serviceTime) {
        return Node.builder()
                .stringId(stringId)
                .type(NodeType.fromChar(typeChar))
                .x(x)
                .y(y)
                .demand(demand)
                .readyTime(readyTime)
                .dueTime(dueTime)
                .serviceTime(serviceTime)
                .build();
    }
}
