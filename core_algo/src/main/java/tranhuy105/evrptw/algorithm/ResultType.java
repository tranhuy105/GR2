package tranhuy105.evrptw.algorithm;

import tranhuy105.evrptw.util.Constants;

/**
 * Result types for operator performance tracking
 */
public enum ResultType {
    NEW_BEST(Constants.SIGMA_NEW_BEST),
    BETTER(Constants.SIGMA_BETTER),
    ACCEPTED_WORSE(Constants.SIGMA_ACCEPTED_WORSE);

    private final int score;

    ResultType(int score) {
        this.score = score;
    }

    public int getScore() {
        return score;
    }
}
