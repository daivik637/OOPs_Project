package com.mumbai.best.app;

import com.mumbai.best.exception.ExpiredPassException;
import com.mumbai.best.exception.InsufficientBalanceException;
import com.mumbai.best.exception.InvalidTripException;
import com.mumbai.best.exception.TransitException;
import com.mumbai.best.model.BusRoute;
import com.mumbai.best.model.BusType;
import com.mumbai.best.model.MonthlyPass;
import com.mumbai.best.model.SingleRideTicket;
import com.mumbai.best.model.Stop;
import com.mumbai.best.model.Ticket;
import com.mumbai.best.model.TouristPass;
import com.mumbai.best.model.Trip;
import com.mumbai.best.service.AuditLogService;
import com.mumbai.best.service.TransitManager;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Main application for the Mumbai BEST Bus Transit Network.
 * Provides interactive QR scan simulation, smart card management,
 * and automated test demonstration.
 */
public class BestTransitApp {
    private static final String LOG_FILE = "data" + File.separator + "daily_transactions.txt";
    private final TransitManager transitManager;
    private final Scanner scanner;

    public BestTransitApp() {
        AuditLogService auditLogService = new AuditLogService(LOG_FILE);
        this.transitManager = new TransitManager(auditLogService);
        this.scanner = new Scanner(System.in);
        initializeSampleNetwork();
    }

    /**
     * Initializes realistic Mumbai BEST bus routes and seed commuter smart cards.
     */
    private void initializeSampleNetwork() {
        // Route A-115: AC Bus (South Mumbai Express)
        List<Stop> stopsA115 = Arrays.asList(
                new Stop("S101", "CSMT (Chhatrapati Shivaji Maharaj Terminus)", 0.0, "Zone 1 - Island City"),
                new Stop("S102", "Hutatma Chowk (Flora Fountain)", 1.2, "Zone 1 - Island City"),
                new Stop("S103", "Churchgate Station", 2.5, "Zone 1 - Island City"),
                new Stop("S104", "Marine Drive (Nariman Point)", 4.2, "Zone 1 - Island City"),
                new Stop("S105", "NCPA Arts Centre", 5.5, "Zone 1 - Island City"),
                new Stop("S106", "Cuffe Parade", 7.8, "Zone 1 - Island City"),
                new Stop("S107", "Backbay Bus Depot", 9.4, "Zone 1 - Island City")
        );
        transitManager.addRoute(new BusRoute("A-115", "CSMT to Backbay Depot via Marine Drive", BusType.AC, stopsA115));

        // Route 123: Non-AC Bus (Colaba to Girgaon & Tardeo)
        List<Stop> stops123 = Arrays.asList(
                new Stop("S201", "R.C. Church (Colaba)", 0.0, "Zone 1 - Island City"),
                new Stop("S202", "Regal Cinema (Gateway of India)", 2.1, "Zone 1 - Island City"),
                new Stop("S203", "Churchgate Station", 4.5, "Zone 1 - Island City"),
                new Stop("S204", "Marine Lines", 6.0, "Zone 1 - Island City"),
                new Stop("S205", "Charni Road Station", 7.5, "Zone 1 - Island City"),
                new Stop("S206", "Girgaon Chowpatty", 9.0, "Zone 1 - Island City"),
                new Stop("S207", "Tardeo Central", 12.0, "Zone 1 - Island City")
        );
        transitManager.addRoute(new BusRoute("123", "Colaba to Tardeo via Girgaon Chowpatty", BusType.NON_AC, stops123));

        // Route C-505: AC Inter-Zone Suburban Express (Bandra to Navi Mumbai)
        List<Stop> stops505 = Arrays.asList(
                new Stop("S301", "Bandra Station West", 0.0, "Zone 2 - Western Suburbs"),
                new Stop("S302", "BKC (Bharat Diamond Bourse)", 3.5, "Zone 2 - Western Suburbs"),
                new Stop("S303", "Kurla Depot", 7.0, "Zone 3 - Eastern Suburbs"),
                new Stop("S304", "Chembur Naka", 11.5, "Zone 3 - Eastern Suburbs"),
                new Stop("S305", "Mankhurd Station", 14.8, "Zone 3 - Eastern Suburbs"),
                new Stop("S306", "Vashi Plaza", 19.5, "Zone 4 - Navi Mumbai"),
                new Stop("S307", "CBD Belapur", 26.5, "Zone 4 - Navi Mumbai")
        );
        transitManager.addRoute(new BusRoute("C-505", "Bandra West to CBD Belapur Inter-Zone Express", BusType.AC, stops505));

        // Seed Sample Commuter Cards / Passes
        // 1. Single Ride Smart Card (Normal balance)
        SingleRideTicket card1 = new SingleRideTicket("CARD-101", "Aarav Mehta", LocalDateTime.now().minusDays(5), 50.0);
        transitManager.registerTicket(card1, 50.0);

        // 2. Single Ride Smart Card with Low Balance (< ₹6 minimum) to test InsufficientBalanceException
        SingleRideTicket card2 = new SingleRideTicket("CARD-102", "Sneha Patel", LocalDateTime.now().minusDays(2), 4.0);
        transitManager.registerTicket(card2, 4.0);

        // 3. Active Monthly Pass (All Zones, AC permitted)
        MonthlyPass pass1 = new MonthlyPass("PASS-201", "Rohan Deshmukh", LocalDateTime.now().minusDays(10),
                LocalDateTime.now().plusDays(20), "ALL", true);
        transitManager.registerTicket(pass1, 750.0);

        // 4. Expired Monthly Pass to test ExpiredPassException
        MonthlyPass passExpired = new MonthlyPass("PASS-202", "Vikram Rane", LocalDateTime.now().minusDays(40),
                LocalDateTime.now().minusDays(2), "ALL", false);
        transitManager.registerTicket(passExpired, 500.0);

        // 5. Tourist Pass (1-Day Unlimited BEST pass)
        TouristPass tour1 = new TouristPass("TOUR-301", "John Smith", LocalDateTime.now(),
                LocalDateTime.now().plusDays(1), "1-Day Mumbai Darshan Unlimited");
        transitManager.registerTicket(tour1, 150.0);
    }

    public static void main(String[] args) {
        BestTransitApp app = new BestTransitApp();
        if (args.length > 0 && "--demo".equalsIgnoreCase(args[0])) {
            app.runAutomatedDemo();
        } else {
            app.runMainMenu();
        }
    }

    public void runMainMenu() {
        System.out.println("======================================================================");
        System.out.println("       WELCOME TO MUMBAI BEST BUS TRANSIT MANAGEMENT SYSTEM           ");
        System.out.println("      Brihanmumbai Electric Supply and Transport (BEST) Undertaking   ");
        System.out.println("======================================================================");

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("Enter choice (0-9): ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> handleTapIn();
                case "2" -> handleTapOut();
                case "3" -> handleIssueCard();
                case "4" -> handleRecharge();
                case "5" -> handleViewAllCards();
                case "6" -> handleViewActiveTrips();
                case "7" -> handleGenerateReport();
                case "8" -> runAutomatedDemo();
                case "9" -> handleViewRawAuditLog();
                case "0" -> {
                    System.out.println("\nThank you for traveling with BEST Mumbai. Have a safe journey!");
                    running = false;
                }
                default -> System.out.println("Invalid selection. Please try again.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n--------------------------- MAIN MENU --------------------------------");
        System.out.println("  [1] Tap-In (Board Bus - Scan QR)");
        System.out.println("  [2] Tap-Out (Alight Bus - Scan QR & Deduct BEST Fare)");
        System.out.println("  [3] Issue New Smart Card / Pass");
        System.out.println("  [4] Recharge Smart Card Wallet");
        System.out.println("  [5] View All Registered Smart Cards & Balances");
        System.out.println("  [6] View Active In-Flight Trips");
        System.out.println("  [7] Generate & View Daily Revenue Audit Report (File Handling)");
        System.out.println("  [8] Run Comprehensive Automated System Demo (OOP & Exceptions)");
        System.out.println("  [9] View Raw Audit Log File (" + LOG_FILE + ")");
        System.out.println("  [0] Exit");
        System.out.println("----------------------------------------------------------------------");
    }

    private void handleTapIn() {
        System.out.println("\n--- [TAP-IN] SCAN BUS QR CODE ON BOARDING ---");
        System.out.print("Scan / Enter Ticket or Card ID (e.g., CARD-101, PASS-201): ");
        String ticketId = scanner.nextLine().trim();

        System.out.println("\nAvailable BEST Routes:");
        for (BusRoute r : transitManager.getAllRoutes()) {
            System.out.println("  • " + r.getRouteNumber() + " : " + r.getRouteName() + " (" + r.getBusType().getDisplayName() + ")");
        }
        System.out.print("Enter Route Number: ");
        String routeNum = scanner.nextLine().trim();

        BusRoute route = transitManager.getRoute(routeNum);
        if (route == null) {
            System.out.println("❌ Error: Route not found.");
            return;
        }

        System.out.println("\nStops on Route " + route.getRouteNumber() + ":");
        for (Stop s : route.getStops()) {
            System.out.println("  [" + s.getStopId() + "] " + s.getName() + " (" + s.getDistanceFromOriginKm() + " km)");
        }
        System.out.print("Enter Boarding Stop ID: ");
        String stopId = scanner.nextLine().trim();

        try {
            Trip trip = transitManager.tapIn(ticketId, routeNum, stopId);
            System.out.println("\n✅ TAP-IN SUCCESSFUL!");
            System.out.println("   " + trip);
            System.out.println("   Welcome aboard BEST Route " + route.getRouteNumber() + "!");
        } catch (InsufficientBalanceException e) {
            System.out.println("\n❌ TAP-IN DENIED [Insufficient Balance]: " + e.getMessage());
            System.out.println("   Please recharge your smart card to continue.");
        } catch (ExpiredPassException e) {
            System.out.println("\n❌ TAP-IN DENIED [Expired Pass]: " + e.getMessage());
        } catch (InvalidTripException e) {
            System.out.println("\n❌ TAP-IN ERROR [Invalid Trip]: " + e.getMessage());
        } catch (TransitException e) {
            System.out.println("\n❌ TAP-IN FAILED: " + e.getMessage());
        }
    }

    private void handleTapOut() {
        System.out.println("\n--- [TAP-OUT] SCAN BUS QR CODE ON ALIGHTING ---");
        System.out.print("Scan / Enter Ticket or Card ID: ");
        String ticketId = scanner.nextLine().trim();

        Trip activeTrip = transitManager.getActiveTrips().get(ticketId.toUpperCase());
        if (activeTrip == null) {
            System.out.println("❌ Error: No active trip in progress for this card. Please tap-in first.");
            return;
        }

        BusRoute route = activeTrip.getRoute();
        System.out.println("\nYou boarded Route " + route.getRouteNumber() + " at: " + activeTrip.getBoardingStop().getName());
        System.out.println("Stops on Route " + route.getRouteNumber() + ":");
        for (Stop s : route.getStops()) {
            System.out.println("  [" + s.getStopId() + "] " + s.getName() + " (" + s.getDistanceFromOriginKm() + " km)");
        }
        System.out.print("Enter Alighting Stop ID: ");
        String stopId = scanner.nextLine().trim();

        try {
            Trip completedTrip = transitManager.tapOut(ticketId, route.getRouteNumber(), stopId);
            System.out.println("\n✅ TAP-OUT SUCCESSFUL!");
            System.out.println("   " + completedTrip);
            System.out.println("   Distance Traveled: " + String.format("%.2f km", completedTrip.getDistanceKm()));
            System.out.println("   BEST Fare Calculated: ₹" + String.format("%.2f", completedTrip.getFareCharged()));

            Ticket ticket = completedTrip.getTicket();
            if (ticket instanceof SingleRideTicket singleRideTicket) {
                System.out.println("   Updated Card Balance: ₹" + String.format("%.2f", singleRideTicket.getWalletBalance()));
            } else {
                System.out.println("   " + ticket.getTicketType() + " verified. ₹0.00 deducted from pass!");
            }
        } catch (InsufficientBalanceException e) {
            System.out.println("\n❌ TAP-OUT FAILED [Insufficient Balance]: " + e.getMessage());
        } catch (TransitException e) {
            System.out.println("\n❌ TAP-OUT FAILED: " + e.getMessage());
        }
    }

    private void handleIssueCard() {
        System.out.println("\n--- ISSUE NEW SMART CARD / PASS ---");
        System.out.println("  1. Single Ride Smart Card (Wallet)");
        System.out.println("  2. Monthly Unlimited Commuter Pass");
        System.out.println("  3. Tourist Pass (Prepaid Mumbai Darshan)");
        System.out.print("Select Card Type (1-3): ");
        String typeChoice = scanner.nextLine().trim();

        System.out.print("Enter Commuter Full Name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) name = "Mumbaikar";

        long timestamp = System.currentTimeMillis() % 10000;
        switch (typeChoice) {
            case "1" -> {
                String cardId = "CARD-" + timestamp;
                System.out.print("Enter Initial Wallet Recharge Amount (₹, min 10): ");
                double amount = parseDouble(scanner.nextLine().trim(), 50.0);
                SingleRideTicket card = new SingleRideTicket(cardId, name, LocalDateTime.now(), amount);
                transitManager.registerTicket(card, amount);
                System.out.println("✅ Issued " + card.getTicketType() + " [" + cardId + "] with ₹" + amount + " balance.");
            }
            case "2" -> {
                String passId = "PASS-" + timestamp;
                System.out.print("Is AC Bus allowed? (y/n, default y): ");
                boolean ac = !scanner.nextLine().trim().equalsIgnoreCase("n");
                System.out.print("Allowed Zone (ALL or Zone 1, default ALL): ");
                String zone = scanner.nextLine().trim();
                if (zone.isEmpty()) zone = "ALL";
                double cost = ac ? 750.0 : 500.0;
                MonthlyPass pass = new MonthlyPass(passId, name, LocalDateTime.now(),
                        LocalDateTime.now().plusMonths(1), zone, ac);
                transitManager.registerTicket(pass, cost);
                System.out.println("✅ Issued " + pass.getTicketType() + " [" + passId + "] valid for 30 days. Subscription Fee: ₹" + cost);
            }
            case "3" -> {
                String tourId = "TOUR-" + timestamp;
                System.out.print("Enter Pass Validity Days (1 or 3, default 1): ");
                int days = parseInt(scanner.nextLine().trim(), 1);
                double cost = days * 150.0;
                TouristPass touristPass = new TouristPass(tourId, name, LocalDateTime.now(),
                        LocalDateTime.now().plusDays(days), days + "-Day Tourist Unlimited Pass");
                transitManager.registerTicket(touristPass, cost);
                System.out.println("✅ Issued " + touristPass.getTicketType() + " [" + tourId + "]. Pass Fee: ₹" + cost);
            }
            default -> System.out.println("Invalid selection.");
        }
    }

    private void handleRecharge() {
        System.out.println("\n--- RECHARGE SMART CARD WALLET ---");
        System.out.print("Enter Smart Card ID: ");
        String cardId = scanner.nextLine().trim();
        System.out.print("Enter Amount to Recharge (₹): ");
        double amount = parseDouble(scanner.nextLine().trim(), 0.0);

        try {
            transitManager.rechargeCard(cardId, amount);
            SingleRideTicket card = (SingleRideTicket) transitManager.getTicket(cardId);
            System.out.println("✅ Recharge Successful! New Balance: ₹" + String.format("%.2f", card.getWalletBalance()));
        } catch (TransitException e) {
            System.out.println("❌ Recharge Failed: " + e.getMessage());
        }
    }

    private void handleViewAllCards() {
        System.out.println("\n================ REGISTERED SMART CARDS & PASSES ================");
        for (Ticket t : transitManager.getAllTickets()) {
            System.out.println(" • " + t.getDetails());
        }
        System.out.println("=================================================================");
    }

    private void handleViewActiveTrips() {
        System.out.println("\n================ IN-FLIGHT ACTIVE TRIPS ================");
        if (transitManager.getActiveTrips().isEmpty()) {
            System.out.println("No commuter currently onboard any bus.");
        } else {
            for (Trip t : transitManager.getActiveTrips().values()) {
                System.out.println(" • " + t);
            }
        }
        System.out.println("========================================================");
    }

    private void handleGenerateReport() {
        try {
            System.out.println("\nGenerating report from daily transaction audit log: " + LOG_FILE + " ...");
            AuditLogService.DailyRevenueReport report = transitManager.getAuditLogService().generateDailyRevenueReport();
            System.out.println(report.formatReport());
        } catch (IOException e) {
            System.out.println("❌ Error reading audit log file: " + e.getMessage());
        }
    }

    private void handleViewRawAuditLog() {
        try {
            List<String> logs = transitManager.getAuditLogService().readAllLogs();
            System.out.println("\n================ RAW TRANSACTION AUDIT LOG FILE ================");
            System.out.println("File: " + transitManager.getAuditLogService().getLogFilePath() + " (" + logs.size() + " lines)\n");
            for (String line : logs) {
                System.out.println(line);
            }
            System.out.println("================================================================");
        } catch (IOException e) {
            System.out.println("❌ Error reading audit log file: " + e.getMessage());
        }
    }

    /**
     * Executes an end-to-end automated demonstration highlighting:
     * 1. Dynamic BEST Fare calculation for Non-AC and AC routes.
     * 2. SingleRideTicket wallet deduction.
     * 3. MonthlyPass and TouristPass zero-deduction polymorphism.
     * 4. InsufficientBalanceException throw and catch.
     * 5. ExpiredPassException throw and catch.
     * 6. InvalidTripException (duplicate tap-in and invalid tap-out) throw and catch.
     * 7. Audit log generation and reading for Daily Revenue Reports.
     */
    public void runAutomatedDemo() {
        System.out.println("\n======================================================================");
        System.out.println("  STARTING AUTOMATED END-TO-END DEMO: BEST BUS TRANSIT & OOP FEATURES ");
        System.out.println("======================================================================");

        try {
            // Scenario 1: Standard Single Ride on AC Bus (Route A-115)
            System.out.println("\n[SCENARIO 1] Single Ride Card Tap-In & Tap-Out (AC Route A-115)");
            SingleRideTicket card1 = (SingleRideTicket) transitManager.getTicket("CARD-101");
            System.out.printf("Commuter: %s | Initial Balance: ₹%.2f\n", card1.getCommuterName(), card1.getWalletBalance());

            // Tap in at CSMT (0.0 km)
            System.out.println("-> Scanning QR at CSMT (Entry)...");
            Trip trip1 = transitManager.tapIn("CARD-101", "A-115", "S101");
            System.out.println("   Tap-in approved: " + trip1);

            // Tap out at Marine Drive (4.2 km)
            System.out.println("-> Scanning QR at Marine Drive (Exit)...");
            Trip compTrip1 = transitManager.tapOut("CARD-101", "A-115", "S104");
            System.out.printf("   Trip completed! Distance: %.2f km | BEST AC Fare Slab: ₹%.2f\n",
                    compTrip1.getDistanceKm(), compTrip1.getFareCharged());
            System.out.printf("   Updated Wallet Balance: ₹%.2f (Deducted ₹%.2f)\n",
                    card1.getWalletBalance(), compTrip1.getFareCharged());

            // Scenario 2: Single Ride on Longer Non-AC Route (Route 123)
            System.out.println("\n[SCENARIO 2] Single Ride Card Tap-In & Tap-Out (Non-AC Route 123, 12 km)");
            System.out.println("-> Scanning QR at R.C. Church (0.0 km)...");
            transitManager.tapIn("CARD-101", "123", "S201");
            System.out.println("-> Scanning QR at Tardeo Central (12.0 km)...");
            Trip compTrip2 = transitManager.tapOut("CARD-101", "123", "S207");
            System.out.printf("   Trip completed! Distance: %.2f km | BEST Non-AC Fare (10-15km slab): ₹%.2f\n",
                    compTrip2.getDistanceKm(), compTrip2.getFareCharged());
            System.out.printf("   Updated Wallet Balance: ₹%.2f\n", card1.getWalletBalance());

            // Scenario 3: Monthly Pass Holder (Route A-115) - Polymorphism (₹0 Fare)
            System.out.println("\n[SCENARIO 3] Monthly Pass Tap-In & Tap-Out (Polymorphic ₹0.00 Deduction)");
            MonthlyPass pass1 = (MonthlyPass) transitManager.getTicket("PASS-201");
            System.out.println("Commuter: " + pass1.getCommuterName() + " | Pass: " + pass1.getDetails());
            transitManager.tapIn("PASS-201", "A-115", "S101");
            Trip compTrip3 = transitManager.tapOut("PASS-201", "A-115", "S107");
            System.out.printf("   Monthly Pass Trip: Distance: %.2f km | Fare Charged: ₹%.2f | Completed Trips: %d\n",
                    compTrip3.getDistanceKm(), compTrip3.getFareCharged(), pass1.getTripsCompleted());

            // Scenario 4: Tourist Pass Holder (Route C-505) - Unlimited Free Darshan
            System.out.println("\n[SCENARIO 4] Tourist Pass Tap-In & Tap-Out (All Mumbai Hop-On Hop-Off)");
            TouristPass tour1 = (TouristPass) transitManager.getTicket("TOUR-301");
            System.out.println("Commuter: " + tour1.getCommuterName() + " | " + tour1.getTicketType());
            transitManager.tapIn("TOUR-301", "C-505", "S301");
            Trip compTrip4 = transitManager.tapOut("TOUR-301", "C-505", "S307");
            System.out.printf("   Tourist Trip (Bandra to Belapur): Distance: %.2f km | Fare Charged: ₹%.2f\n",
                    compTrip4.getDistanceKm(), compTrip4.getFareCharged());

            // Scenario 5: Exception Handling - InsufficientBalanceException
            System.out.println("\n[SCENARIO 5] Exception Handling: InsufficientBalanceException");
            SingleRideTicket lowCard = (SingleRideTicket) transitManager.getTicket("CARD-102");
            System.out.printf("Commuter: %s | Balance: ₹%.2f (Less than minimum ₹6.00)\n",
                    lowCard.getCommuterName(), lowCard.getWalletBalance());
            try {
                System.out.println("-> Attempting Tap-In on Route 123...");
                transitManager.tapIn("CARD-102", "123", "S201");
            } catch (InsufficientBalanceException e) {
                System.out.println("   [CAUGHT EXPECTED EXCEPTION] " + e.getMessage());
                System.out.printf("   Current: ₹%.2f, Required: ₹%.2f\n", e.getCurrentBalance(), e.getRequiredAmount());
            }

            // Recharge card to recover from exception
            System.out.println("-> Commuter recharges ₹50.00 to resolve insufficient balance...");
            transitManager.rechargeCard("CARD-102", 50.0);
            System.out.println("-> Retrying Tap-In after recharge...");
            transitManager.tapIn("CARD-102", "123", "S201");
            Trip compTrip5 = transitManager.tapOut("CARD-102", "123", "S203");
            System.out.printf("   Tap-in & Tap-out successful after recharge! Fare: ₹%.2f | Balance: ₹%.2f\n",
                    compTrip5.getFareCharged(), lowCard.getWalletBalance());

            // Scenario 6: Exception Handling - ExpiredPassException
            System.out.println("\n[SCENARIO 6] Exception Handling: ExpiredPassException");
            MonthlyPass expiredPass = (MonthlyPass) transitManager.getTicket("PASS-202");
            System.out.println("Commuter: " + expiredPass.getCommuterName() + " | " + expiredPass.getDetails());
            try {
                System.out.println("-> Attempting Tap-In with expired pass on Route A-115...");
                transitManager.tapIn("PASS-202", "A-115", "S101");
            } catch (ExpiredPassException e) {
                System.out.println("   [CAUGHT EXPECTED EXCEPTION] " + e.getMessage());
            }

            // Scenario 7: Exception Handling - InvalidTripException (Tap-Out without Tap-In)
            System.out.println("\n[SCENARIO 7] Exception Handling: InvalidTripException (Tap-Out Without Tap-In)");
            try {
                System.out.println("-> Scanning Tap-Out on CARD-101 without prior board scan...");
                transitManager.tapOut("CARD-101", "A-115", "S105");
            } catch (InvalidTripException e) {
                System.out.println("   [CAUGHT EXPECTED EXCEPTION] " + e.getMessage());
            }

            // Scenario 8: File Handling & Revenue Report
            System.out.println("\n[SCENARIO 8] File Handling: Audit Log Reading & Daily Revenue Report Generation");
            AuditLogService.DailyRevenueReport report = transitManager.getAuditLogService().generateDailyRevenueReport();
            System.out.println(report.formatReport());

            System.out.println("✅ ALL DEMO SCENARIOS COMPLETED SUCCESSFULLY!");
        } catch (Exception e) {
            System.err.println("Unexpected error during demo execution: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static double parseDouble(String str, double defaultVal) {
        try {
            return Double.parseDouble(str);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private static int parseInt(String str, int defaultVal) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            return defaultVal;
        }
    }
}
