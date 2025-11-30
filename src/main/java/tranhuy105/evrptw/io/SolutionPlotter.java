package tranhuy105.evrptw.io;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import tranhuy105.evrptw.model.Instance;
import tranhuy105.evrptw.model.Node;
import tranhuy105.evrptw.model.NodeType;
import tranhuy105.evrptw.model.Solution;
import tranhuy105.evrptw.util.Logger;

/**
 * Plots EVRPTW solutions to PNG images
 */
public class SolutionPlotter {
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 1000;
    private static final int MARGIN = 80;
    private static final int NODE_SIZE = 12;
    private static final int DEPOT_SIZE = 18;
    private static final int STATION_SIZE = 10;

    // Color palette for routes
    private static final Color[] ROUTE_COLORS = {
        new Color(31, 119, 180),   // Blue
        new Color(255, 127, 14),   // Orange
        new Color(44, 160, 44),    // Green
        new Color(214, 39, 40),    // Red
        new Color(148, 103, 189),  // Purple
        new Color(140, 86, 75),    // Brown
        new Color(227, 119, 194),  // Pink
        new Color(127, 127, 127),  // Gray
        new Color(188, 189, 34),   // Olive
        new Color(23, 190, 207),   // Cyan
    };

    /**
     * Plot solution and save to file
     */
    public void plot(Solution solution, String filepath) throws IOException {
        Instance inst = solution.getInstance();
        
        // Ensure output directory exists
        Path path = Path.of(filepath);
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        // Create image
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // White background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Calculate coordinate bounds
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;

        for (Node node : inst.getAllNodes()) {
            minX = Math.min(minX, node.getX());
            maxX = Math.max(maxX, node.getX());
            minY = Math.min(minY, node.getY());
            maxY = Math.max(maxY, node.getY());
        }

        // Add padding
        double rangeX = maxX - minX;
        double rangeY = maxY - minY;
        minX -= rangeX * 0.05;
        maxX += rangeX * 0.05;
        minY -= rangeY * 0.05;
        maxY += rangeY * 0.05;

        // Coordinate transformation
        double scaleX = (WIDTH - 2 * MARGIN) / (maxX - minX);
        double scaleY = (HEIGHT - 2 * MARGIN) / (maxY - minY);
        double scale = Math.min(scaleX, scaleY);

        double offsetX = MARGIN + (WIDTH - 2 * MARGIN - scale * (maxX - minX)) / 2;
        double offsetY = MARGIN + (HEIGHT - 2 * MARGIN - scale * (maxY - minY)) / 2;

        // Draw routes
        g2d.setStroke(new BasicStroke(2.0f));
        int routeIdx = 0;
        for (List<Integer> route : solution.getRoutes()) {
            Color routeColor = ROUTE_COLORS[routeIdx % ROUTE_COLORS.length];
            g2d.setColor(routeColor);

            // Draw from depot
            Node depot = inst.getDepot();
            int prevX = toScreenX(depot.getX(), minX, scale, offsetX);
            int prevY = toScreenY(depot.getY(), minY, maxY, scale, offsetY);

            for (int nodeId : route) {
                Node node = inst.getAllNodes().get(nodeId);
                int currX = toScreenX(node.getX(), minX, scale, offsetX);
                int currY = toScreenY(node.getY(), minY, maxY, scale, offsetY);
                g2d.drawLine(prevX, prevY, currX, currY);
                prevX = currX;
                prevY = currY;
            }

            // Draw back to depot
            int depotX = toScreenX(depot.getX(), minX, scale, offsetX);
            int depotY = toScreenY(depot.getY(), minY, maxY, scale, offsetY);
            g2d.drawLine(prevX, prevY, depotX, depotY);

            routeIdx++;
        }

        // Draw nodes
        for (Node node : inst.getAllNodes()) {
            int x = toScreenX(node.getX(), minX, scale, offsetX);
            int y = toScreenY(node.getY(), minY, maxY, scale, offsetY);

            if (node.getType() == NodeType.DEPOT) {
                // Depot: large red square
                g2d.setColor(Color.RED);
                g2d.fillRect(x - DEPOT_SIZE/2, y - DEPOT_SIZE/2, DEPOT_SIZE, DEPOT_SIZE);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x - DEPOT_SIZE/2, y - DEPOT_SIZE/2, DEPOT_SIZE, DEPOT_SIZE);
            } else if (node.getType() == NodeType.STATION) {
                // Station: green triangle
                g2d.setColor(new Color(0, 200, 0));
                int[] xPoints = {x, x - STATION_SIZE, x + STATION_SIZE};
                int[] yPoints = {y - STATION_SIZE, y + STATION_SIZE, y + STATION_SIZE};
                g2d.fillPolygon(xPoints, yPoints, 3);
                g2d.setColor(Color.BLACK);
                g2d.drawPolygon(xPoints, yPoints, 3);
            } else {
                // Customer: blue circle
                g2d.setColor(new Color(70, 130, 180));
                g2d.fillOval(x - NODE_SIZE/2, y - NODE_SIZE/2, NODE_SIZE, NODE_SIZE);
                g2d.setColor(Color.BLACK);
                g2d.drawOval(x - NODE_SIZE/2, y - NODE_SIZE/2, NODE_SIZE, NODE_SIZE);
            }
        }

        // Draw title and stats
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
        String title = String.format("EVRPTW Solution - %d vehicles, Distance: %.2f",
                solution.getRoutes().size(), solution.getTotalDistance());
        g2d.drawString(title, MARGIN, 30);

        // Draw legend
        drawLegend(g2d, WIDTH - MARGIN - 150, 50);

        g2d.dispose();

        // Save image
        ImageIO.write(image, "PNG", new File(filepath));
        Logger.info("Solution plot saved to: " + filepath);
    }

    private int toScreenX(double x, double minX, double scale, double offsetX) {
        return (int) (offsetX + (x - minX) * scale);
    }

    @SuppressWarnings("unused")
    private int toScreenY(double y, double minY, double maxY, double scale, double offsetY) {
        // Flip Y axis (screen Y increases downward)
        // Note: minY is kept for API consistency with toScreenX
        return (int) (offsetY + (maxY - y) * scale);
    }

    private void drawLegend(Graphics2D g2d, int x, int y) {
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        
        // Depot
        g2d.setColor(Color.RED);
        g2d.fillRect(x, y, 12, 12);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y, 12, 12);
        g2d.drawString("Depot", x + 20, y + 11);

        // Customer
        g2d.setColor(new Color(70, 130, 180));
        g2d.fillOval(x, y + 20, 12, 12);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(x, y + 20, 12, 12);
        g2d.drawString("Customer", x + 20, y + 31);

        // Station
        g2d.setColor(new Color(0, 200, 0));
        int[] xPoints = {x + 6, x, x + 12};
        int[] yPoints = {y + 40, y + 52, y + 52};
        g2d.fillPolygon(xPoints, yPoints, 3);
        g2d.setColor(Color.BLACK);
        g2d.drawPolygon(xPoints, yPoints, 3);
        g2d.drawString("Station", x + 20, y + 51);
    }
}
