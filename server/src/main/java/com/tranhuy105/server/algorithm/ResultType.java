package com.tranhuy105.server.algorithm;

/**
 * Result types for operator performance tracking
 */
public enum ResultType {
    NEW_BEST(33),
    BETTER(9),
    ACCEPTED_WORSE(13);

    private final int score;

    ResultType(int score) {
        this.score = score;
    }

    public int getScore() {
        return score;
    }
}
