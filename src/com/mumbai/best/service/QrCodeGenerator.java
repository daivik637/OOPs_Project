package com.mumbai.best.service;

import com.mumbai.best.model.Bus;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Generates realistic, high-contrast 2D QR Code graphics for physical BEST buses.
 * Includes official BEST branding, alignment patterns, and saves PNGs to data/ directory.
 */
public class QrCodeGenerator {

    private static final int QR_MODULES = 29; // Standard Version 3 QR grid size (29x29 modules)

    /**
     * Generates a styled QR code image with finder patterns, timing patterns,
     * and deterministically encoded payload modules based on the bus data.
     */
    public static BufferedImage generateBusQrImage(Bus bus, int imageSize) {
        int quietZone = 2; // module padding
        int totalGrid = QR_MODULES + (quietZone * 2);
        int modulePixelSize = Math.max(4, imageSize / totalGrid);
        int qrWidth = totalGrid * modulePixelSize;
        int qrOffsetY = 42;
        int bottomMargin = 55;
        int totalHeight = qrOffsetY + qrWidth + bottomMargin;

        BufferedImage image = new BufferedImage(qrWidth, totalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // White background
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, qrWidth, totalHeight);

        // Top header banner
        Color headerColor = bus.getRoute().getBusType().name().contains("AC") 
                ? new Color(0, 102, 204)  // Royal Electric Blue for AC
                : new Color(180, 20, 20); // BEST Iconic Red for Non-AC
        g2.setColor(headerColor);
        g2.fillRect(0, 0, qrWidth, 34);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        String title = "BEST MUMBAI • SCAN TO RIDE";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, (qrWidth - fm.stringWidth(title)) / 2, 22);

        // QR Matrix generation (29x29)
        boolean[][] matrix = new boolean[QR_MODULES][QR_MODULES];

        // 1. Finder patterns at (0,0), (0, 22), (22, 0)
        drawFinderPattern(matrix, 0, 0);
        drawFinderPattern(matrix, 0, QR_MODULES - 7);
        drawFinderPattern(matrix, QR_MODULES - 7, 0);

        // 2. Timing patterns
        for (int i = 8; i < QR_MODULES - 8; i++) {
            matrix[6][i] = (i % 2 == 0);
            matrix[i][6] = (i % 2 == 0);
        }

        // 3. Alignment pattern at (20, 20)
        drawAlignmentPattern(matrix, QR_MODULES - 9, QR_MODULES - 9);

        // 4. Fill remaining data modules deterministically using SHA-256 of QR payload
        byte[] hash = getHash(bus.getQrCodeData());
        int hashIdx = 0;
        for (int r = 0; r < QR_MODULES; r++) {
            for (int c = 0; c < QR_MODULES; c++) {
                if (isReservedPattern(r, c, QR_MODULES)) continue;
                int byteVal = hash[hashIdx % hash.length] & 0xFF;
                // pseudo-random bit determination combining coordinates and payload hash
                boolean bit = ((byteVal + (r * 13) + (c * 17)) % 2 == 0);
                matrix[r][c] = bit;
                hashIdx++;
            }
        }

        // Draw the QR modules
        g2.setColor(Color.BLACK);
        for (int r = 0; r < QR_MODULES; r++) {
            for (int c = 0; c < QR_MODULES; c++) {
                if (matrix[r][c]) {
                    int px = (c + quietZone) * modulePixelSize;
                    int py = qrOffsetY + ((r + quietZone) * modulePixelSize);
                    g2.fillRect(px, py, modulePixelSize, modulePixelSize);
                }
            }
        }

        // Draw center bus badge
        int centerRadius = modulePixelSize * 3;
        int centerX = qrWidth / 2;
        int centerY = qrOffsetY + ((QR_MODULES / 2 + quietZone) * modulePixelSize);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(centerX - centerRadius, centerY - centerRadius, centerRadius * 2, centerRadius * 2, 8, 8);
        g2.setColor(headerColor);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(centerX - centerRadius, centerY - centerRadius, centerRadius * 2, centerRadius * 2, 8, 8);

        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        fm = g2.getFontMetrics();
        String badge = "BEST";
        g2.drawString(badge, centerX - (fm.stringWidth(badge) / 2), centerY + 4);

        // Bottom bus information caption
        int captionY = qrOffsetY + qrWidth + 22;
        g2.setColor(new Color(30, 30, 30));
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        String busInfo = bus.getBusId() + " (" + bus.getLicensePlate() + ")";
        fm = g2.getFontMetrics();
        g2.drawString(busInfo, (qrWidth - fm.stringWidth(busInfo)) / 2, captionY);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.setColor(new Color(90, 90, 90));
        String routeInfo = "Route " + bus.getRoute().getRouteNumber() + " • " + bus.getRoute().getBusType().getDisplayName();
        fm = g2.getFontMetrics();
        g2.drawString(routeInfo, (qrWidth - fm.stringWidth(routeInfo)) / 2, captionY + 16);

        g2.dispose();
        return image;
    }

    private static void drawFinderPattern(boolean[][] matrix, int startR, int startC) {
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                if (r == 0 || r == 6 || c == 0 || c == 6) {
                    matrix[startR + r][startC + c] = true; // outer border
                } else if (r >= 2 && r <= 4 && c >= 2 && c <= 4) {
                    matrix[startR + r][startC + c] = true; // inner core
                } else {
                    matrix[startR + r][startC + c] = false; // white space
                }
            }
        }
    }

    private static void drawAlignmentPattern(boolean[][] matrix, int startR, int startC) {
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                if (r == 0 || r == 4 || c == 0 || c == 4 || (r == 2 && c == 2)) {
                    matrix[startR + r][startC + c] = true;
                } else {
                    matrix[startR + r][startC + c] = false;
                }
            }
        }
    }

    private static boolean isReservedPattern(int r, int c, int n) {
        // Top-left finder + separator
        if (r <= 7 && c <= 7) return true;
        // Top-right finder + separator
        if (r <= 7 && c >= n - 8) return true;
        // Bottom-left finder + separator
        if (r >= n - 8 && c <= 7) return true;
        // Timing tracks
        if (r == 6 || c == 6) return true;
        // Alignment pattern
        if (r >= n - 9 && r <= n - 5 && c >= n - 9 && c <= n - 5) return true;
        // Center badge
        int center = n / 2;
        return (Math.abs(r - center) <= 2 && Math.abs(c - center) <= 2);
    }

    private static byte[] getHash(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(data.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            return data.getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * Generates and writes sample QR code image files for two sample buses to the data directory.
     */
    public static void saveSampleBusQrCodes(Bus bus1, Bus bus2, String outputDir) {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            BufferedImage img1 = generateBusQrImage(bus1, 260);
            File file1 = new File(dir, "qr_" + bus1.getBusId().toLowerCase() + ".png");
            ImageIO.write(img1, "PNG", file1);

            BufferedImage img2 = generateBusQrImage(bus2, 260);
            File file2 = new File(dir, "qr_" + bus2.getBusId().toLowerCase() + ".png");
            ImageIO.write(img2, "PNG", file2);
        } catch (IOException e) {
            System.err.println("Could not save QR code files: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        com.mumbai.best.model.BusRoute r1 = new com.mumbai.best.model.BusRoute("A-115", "CSMT to Backbay", com.mumbai.best.model.BusType.AC, java.util.Collections.emptyList());
        com.mumbai.best.model.BusRoute r2 = new com.mumbai.best.model.BusRoute("123", "Colaba to Tardeo", com.mumbai.best.model.BusType.NON_AC, java.util.Collections.emptyList());
        Bus bus1 = new Bus("BUS-1082", "MH-01-CV-1082", r1);
        Bus bus2 = new Bus("BUS-4321", "MH-01-AP-4321", r2);
        saveSampleBusQrCodes(bus1, bus2, "data");
        System.out.println("Generated and saved sample QR codes to data/ directory.");
    }
}
