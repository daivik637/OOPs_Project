package com.mumbai.best.service;

import com.mumbai.best.model.Trip;
import com.mumbai.best.model.Ticket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles persistent file logging of all transit transactions and provides
 * log reading and aggregation for daily revenue and audit reporting.
 * Demonstrates robust Java File Handling (I/O) and try-with-resources.
 */
public class AuditLogService {
    private final String logFilePath;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AuditLogService(String logFilePath) {
        this.logFilePath = logFilePath;
        ensureLogDirectoryExists();
    }

    private void ensureLogDirectoryExists() {
        File file = new File(logFilePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    /**
     * Appends a completed trip transaction to the daily audit log file.
     */
    public synchronized void logTripCompleted(Trip trip) {
        String record = String.format("%s|TRIP_COMPLETED|%s|%s|%s|%s|%s|%s|%s|%.2f|%.2f|SUCCESS",
                trip.getTapOutTime().format(ISO_FORMATTER),
                trip.getTicket().getTicketId(),
                trip.getTicket().getCommuterName(),
                trip.getTicket().getTicketType(),
                trip.getRoute().getRouteNumber(),
                trip.getRoute().getBusType().name(),
                trip.getBoardingStop().getName(),
                trip.getAlightingStop().getName(),
                trip.getDistanceKm(),
                trip.getFareCharged()
        );
        writeLineToFile(record);
    }

    /**
     * Appends a card recharge event to the audit log.
     */
    public synchronized void logRecharge(Ticket ticket, double amount, double newBalance) {
        String record = String.format("%s|RECHARGE|%s|%s|%s|N/A|N/A|N/A|N/A|0.00|%.2f|NEW_BALANCE_₹%.2f",
                LocalDateTime.now().format(ISO_FORMATTER),
                ticket.getTicketId(),
                ticket.getCommuterName(),
                ticket.getTicketType(),
                amount,
                newBalance
        );
        writeLineToFile(record);
    }

    /**
     * Appends a card issuance transaction to the audit log.
     */
    public synchronized void logCardIssued(Ticket ticket, double initialAmount) {
        String record = String.format("%s|CARD_ISSUED|%s|%s|%s|N/A|N/A|N/A|N/A|0.00|%.2f|ISSUED_SUCCESSFULLY",
                ticket.getIssuedAt().format(ISO_FORMATTER),
                ticket.getTicketId(),
                ticket.getCommuterName(),
                ticket.getTicketType(),
                initialAmount
        );
        writeLineToFile(record);
    }

    /**
     * Appends a rejected / failed tap-in event to the audit log.
     */
    public synchronized void logInfraction(String ticketId, String commuterName, String routeNumber, String reason) {
        String record = String.format("%s|FAILED_ATTEMPT|%s|%s|UNKNOWN|%s|N/A|N/A|N/A|0.00|0.00|ERROR: %s",
                LocalDateTime.now().format(ISO_FORMATTER),
                ticketId,
                commuterName != null ? commuterName : "Guest",
                routeNumber,
                reason.replace("|", "-")
        );
        writeLineToFile(record);
    }

    private void writeLineToFile(String line) {
        try (FileWriter fw = new FileWriter(logFilePath, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(line);
        } catch (IOException e) {
            System.err.println("[AuditLogService] Warning: Could not write to log file: " + e.getMessage());
        }
    }

    /**
     * Reads all raw transaction records from the audit log text file.
     */
    public List<String> readAllLogs() throws IOException {
        List<String> records = new ArrayList<>();
        File file = new File(logFilePath);
        if (!file.exists()) {
            return records;
        }

        try (FileReader fr = new FileReader(file);
             BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty() && !line.startsWith("#")) {
                    records.add(line);
                }
            }
        }
        return records;
    }

    /**
     * Parses the daily transaction log file and compiles a comprehensive daily revenue report.
     * Demonstrates parsing and aggregating file-based data in Java.
     */
    public DailyRevenueReport generateDailyRevenueReport() throws IOException {
        List<String> rawLogs = readAllLogs();

        int totalTrips = 0;
        double totalTripRevenue = 0.0;
        double totalRechargeAmount = 0.0;
        int failedAttempts = 0;

        Map<String, Double> revenueByRoute = new HashMap<>();
        Map<String, Integer> tripsByRoute = new HashMap<>();
        Map<String, Double> revenueByBusType = new HashMap<>();
        Map<String, Integer> tripsByTicketType = new HashMap<>();

        for (String record : rawLogs) {
            String[] parts = record.split("\\|");
            if (parts.length < 11) {
                continue;
            }

            String eventType = parts[1];
            String ticketType = parts[4];
            String routeNum = parts[5];
            String busType = parts[6];
            double amount = 0.0;
            try {
                amount = Double.parseDouble(parts[10]);
            } catch (NumberFormatException ignored) {}

            switch (eventType) {
                case "TRIP_COMPLETED" -> {
                    totalTrips++;
                    totalTripRevenue += amount;

                    revenueByRoute.put(routeNum, revenueByRoute.getOrDefault(routeNum, 0.0) + amount);
                    tripsByRoute.put(routeNum, tripsByRoute.getOrDefault(routeNum, 0) + 1);

                    revenueByBusType.put(busType, revenueByBusType.getOrDefault(busType, 0.0) + amount);
                    tripsByTicketType.put(ticketType, tripsByTicketType.getOrDefault(ticketType, 0) + 1);
                }
                case "RECHARGE", "CARD_ISSUED" -> totalRechargeAmount += amount;
                case "FAILED_ATTEMPT" -> failedAttempts++;
            }
        }

        return new DailyRevenueReport(
                totalTrips,
                totalTripRevenue,
                totalRechargeAmount,
                failedAttempts,
                revenueByRoute,
                tripsByRoute,
                revenueByBusType,
                tripsByTicketType,
                rawLogs.size()
        );
    }

    public String getLogFilePath() {
        return logFilePath;
    }

    /**
     * Data Transfer Object summarizing the daily financial performance of the BEST transit network.
     */
    public record DailyRevenueReport(
            int totalTrips,
            double totalTripRevenue,
            double totalRechargeOrIssuanceRevenue,
            int failedAttempts,
            Map<String, Double> revenueByRoute,
            Map<String, Integer> tripsByRoute,
            Map<String, Double> revenueByBusType,
            Map<String, Integer> tripsByTicketType,
            int totalLogEntries
    ) {
        public String formatReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n======================================================================\n");
            sb.append("         BRIHANMUMBAI ELECTRIC SUPPLY & TRANSPORT (BEST) UNDERTAKING\n");
            sb.append("                     DAILY REVENUE & AUDIT SUMMARY REPORT\n");
            sb.append("======================================================================\n");
            sb.append(String.format("Total Audit Log Records Processed: %d\n", totalLogEntries));
            sb.append(String.format("Successfully Completed Passenger Trips: %d\n", totalTrips));
            sb.append(String.format("Total Fare Collection from Completed Trips: ₹%.2f\n", totalTripRevenue));
            sb.append(String.format("Total Smart Card Recharge / Card Issuance: ₹%.2f\n", totalRechargeOrIssuanceRevenue));
            sb.append(String.format("Total Direct Gross Daily Inflow:            ₹%.2f\n", totalTripRevenue + totalRechargeOrIssuanceRevenue));
            sb.append(String.format("Security / Tap-In Infractions Recorded:     %d\n", failedAttempts));
            sb.append("----------------------------------------------------------------------\n");
            sb.append("REVENUE & RIDERSHIP BY BUS CATEGORY:\n");
            for (Map.Entry<String, Double> entry : revenueByBusType.entrySet()) {
                sb.append(String.format("  • %-15s : ₹%.2f\n", entry.getKey(), entry.getValue()));
            }
            sb.append("----------------------------------------------------------------------\n");
            sb.append("RIDERSHIP BY TICKET / PASS CLASSIFICATION:\n");
            for (Map.Entry<String, Integer> entry : tripsByTicketType.entrySet()) {
                sb.append(String.format("  • %-30s : %d rides\n", entry.getKey(), entry.getValue()));
            }
            sb.append("----------------------------------------------------------------------\n");
            sb.append("ROUTE-WISE FINANCIAL BREAKDOWN:\n");
            for (String route : revenueByRoute.keySet()) {
                sb.append(String.format("  • Route %-10s : %3d trips | Collection: ₹%.2f\n",
                        route,
                        tripsByRoute.getOrDefault(route, 0),
                        revenueByRoute.getOrDefault(route, 0.0)));
            }
            sb.append("======================================================================\n");
            return sb.toString();
        }
    }
}
