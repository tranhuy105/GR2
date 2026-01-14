package com.tranhuy105.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

/**
 * Type-safe configuration properties for ALNS algorithm
 */
@ConfigurationProperties(prefix = "alns")
@Validated
public record ALNSProperties(
    @Min(100) int defaultIterations,
    @Min(0) double defaultTimeLimit,
    @Positive double coolingRate,
    PenaltyConfig penalties,
    ShawConfig shaw,
    int segmentSize,
    int stationRemovalInterval
) {
    public ALNSProperties {
        // Defaults if not specified
        if (penalties == null) {
            penalties = new PenaltyConfig(100000, 10000, 10000, 10000);
        }
        if (shaw == null) {
            shaw = new ShawConfig(new double[]{1.0, 1.0, 1.0, 1.0}, 2.0);
        }
        if (segmentSize <= 0) {
            segmentSize = 100;
        }
        if (stationRemovalInterval <= 0) {
            stationRemovalInterval = 500;
        }
    }

    public record PenaltyConfig(
        double vehicle,
        double capacity,
        double time,
        double battery
    ) {}

    public record ShawConfig(
        double[] phi,
        double eta
    ) {}
}
