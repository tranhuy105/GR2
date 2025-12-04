package tranhuy105.evrptw.model;

/**
 * Charging strategy at stations
 */
public enum ChargingMode {
    /**
     * Full recharge: charging time = (batteryCapacity - currentBattery) * refuelRate
     */
    FULL_RECHARGE,
    
    /**
     * Battery swap: fixed swap time regardless of current battery level
     */
    BATTERY_SWAP
}
