package tranhuy105.evrptw.model;

/**
 * Represents a node in the EVRPTW problem (depot, customer, or charging station)
 */
public class Node {
    private final String stringId;
    private int id;  // Numeric ID assigned during instance finalization
    private final NodeType type;
    private final double x;
    private final double y;
    private final double demand;
    private final double readyTime;
    private final double dueTime;
    private final double serviceTime;

    public Node(String stringId, NodeType type, double x, double y,
                double demand, double readyTime, double dueTime, double serviceTime) {
        this.stringId = stringId;
        this.id = -1;  // Will be set during finalization
        this.type = type;
        this.x = x;
        this.y = y;
        this.demand = demand;
        this.readyTime = readyTime;
        this.dueTime = dueTime;
        this.serviceTime = serviceTime;
    }

    public String getStringId() {
        return stringId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public NodeType getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getDemand() {
        return demand;
    }

    public double getReadyTime() {
        return readyTime;
    }

    public double getDueTime() {
        return dueTime;
    }

    public double getServiceTime() {
        return serviceTime;
    }

    @Override
    public String toString() {
        return String.format("%s[%s](%s)", type, stringId, id);
    }
}
