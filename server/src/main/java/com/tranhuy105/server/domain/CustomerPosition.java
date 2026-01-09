package com.tranhuy105.server.domain;

/**
 * Position of a customer in the solution (for operators)
 */
public record CustomerPosition(
    int routeIndex,
    int position,
    int customerId
) {}
