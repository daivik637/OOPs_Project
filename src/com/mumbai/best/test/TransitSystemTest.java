package com.mumbai.best.test;

import com.mumbai.best.exception.ExpiredPassException;
import com.mumbai.best.exception.InsufficientBalanceException;
import com.mumbai.best.exception.InvalidTripException;
import com.mumbai.best.model.BusRoute;
import com.mumbai.best.model.BusType;
import com.mumbai.best.model.MonthlyPass;
import com.mumbai.best.model.SingleRideTicket;
import com.mumbai.best.model.Stop;
import com.mumbai.best.model.Ticket;
import com.mumbai.best.model.TouristPass;
import com.mumbai.best.model.Trip;
import com.mumbai.best.service.AuditLogService;
import com.mumbai.best.service.FareCalculator;
import com.mumbai.best.service.TransitManager;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Verification test suite validating:
 * 1. BEST Distance Slab calculations for Non-AC & AC buses
 * 2. Polymorphic fare deduction across SingleRideTicket, MonthlyPass, TouristPass
 * 3. Exception handling (InsufficientBalanceException, ExpiredPassException, InvalidTripException)
 * 4. File handling (writing to audit log and parsing daily revenue report)
 */
public class TransitSystemTest {

    private static int testsRun = 0;
    private static int testsPassed = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  RUNNING BEST TRANSIT OO SYSTEM TEST SUITE      ");
        System.out.println("=================================================");

        testFareCalculatorSlabs();
        testSingleRideCardLifecycle();
        testMonthlyPassPolymorphismAndExpiry();
        testTouristPassPrivileges();
        testInvalidTripTransitions();
        testAuditLogPersistenceAndReportGeneration();
        testAutomatedScanBusQr();

        System.out.println("=================================================");
        System.out.printf("Test Summary: %d / %d Tests Passed.\n", testsPassed, testsRun);
        if (testsPassed == testsRun) {
            System.out.println(">>> ALL OO AND FUNCTIONAL TESTS PASSED! <<<");
        } else {
            System.err.println(">>> SOME TESTS FAILED! <<<");
            System.exit(1);
        }
    }

    private static void assertEquals(double expected, double actual, double delta, String message) {
        testsRun++;
        if (Math.abs(expected - actual) <= delta) {
            testsPassed++;
            System.out.println("  [PASS] " + message);
        } else {
            System.err.println("  [FAIL] " + message + " -> Expected: " + expected + ", Actual: " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        testsRun++;
        if (condition) {
            testsPassed++;
            System.out.println("  [PASS] " + message);
        } else {
            System.err.println("  [FAIL] " + message);
        }
    }

    private static void testFareCalculatorSlabs() {
        System.out.println("\n[1] Testing FareCalculator BEST Slabs:");
        // Non-AC slabs
        assertEquals(6.0, FareCalculator.calculateBestFare(3.0, BusType.NON_AC), 0.001, "Non-AC up to 5km should be ₹6");
        assertEquals(10.0, FareCalculator.calculateBestFare(8.5, BusType.NON_AC), 0.001, "Non-AC 5-10km should be ₹10");
        assertEquals(15.0, FareCalculator.calculateBestFare(12.0, BusType.NON_AC), 0.001, "Non-AC 10-15km should be ₹15");
        assertEquals(20.0, FareCalculator.calculateBestFare(18.0, BusType.NON_AC), 0.001, "Non-AC 15-20km should be ₹20");
        assertEquals(35.0, FareCalculator.calculateBestFare(32.0, BusType.NON_AC), 0.001, "Non-AC >30km should be ₹35");

        // AC slabs
        assertEquals(6.0, FareCalculator.calculateBestFare(4.2, BusType.AC), 0.001, "AC up to 5km should be ₹6");
        assertEquals(13.0, FareCalculator.calculateBestFare(7.0, BusType.AC), 0.001, "AC 5-10km should be ₹13");
        assertEquals(19.0, FareCalculator.calculateBestFare(14.0, BusType.AC), 0.001, "AC 10-15km should be ₹19");
        assertEquals(25.0, FareCalculator.calculateBestFare(19.0, BusType.AC), 0.001, "AC 15-20km should be ₹25");
    }

    private static void testSingleRideCardLifecycle() {
        System.out.println("\n[2] Testing SingleRideTicket & InsufficientBalanceException:");
        String testLog = "data" + File.separator + "test_audit_1.txt";
        new File(testLog).delete();
        AuditLogService auditService = new AuditLogService(testLog);
        TransitManager tm = new TransitManager(auditService);

        List<Stop> stops = Arrays.asList(
                new Stop("T1", "CSMT", 0.0, "Zone 1"),
                new Stop("T2", "Churchgate", 2.5, "Zone 1"),
                new Stop("T3", "Dadar", 11.0, "Zone 1")
        );
        tm.addRoute(new BusRoute("R1", "CSMT-Dadar", BusType.NON_AC, stops));

        // Test InsufficientBalanceException on tap-in with low balance
        SingleRideTicket lowCard = new SingleRideTicket("C-LOW", "Test Low", LocalDateTime.now(), 3.0);
        tm.registerTicket(lowCard, 3.0);

        boolean caughtTapIn = false;
        try {
            tm.tapIn("C-LOW", "R1", "T1");
        } catch (InsufficientBalanceException e) {
            caughtTapIn = true;
            assertEquals(3.0, e.getCurrentBalance(), 0.001, "InsufficientBalanceException current balance");
            assertEquals(6.0, e.getRequiredAmount(), 0.001, "InsufficientBalanceException required balance");
        } catch (Exception e) {
            System.err.println("Unexpected exception: " + e);
        }
        assertTrue(caughtTapIn, "Tap-in with balance < ₹6 throws InsufficientBalanceException");

        // Recharge and complete trip
        try {
            tm.rechargeCard("C-LOW", 20.0);
            assertEquals(23.0, lowCard.getWalletBalance(), 0.001, "Balance updated to ₹23 after recharge");

            Trip tIn = tm.tapIn("C-LOW", "R1", "T1");
            assertTrue(tIn != null && !tIn.isCompleted(), "Trip is in-flight");

            Trip tOut = tm.tapOut("C-LOW", "R1", "T2"); // 2.5 km -> fare ₹6
            assertTrue(tOut.isCompleted(), "Trip marked completed");
            assertEquals(6.0, tOut.getFareCharged(), 0.001, "Fare ₹6 charged for 2.5 km");
            assertEquals(17.0, lowCard.getWalletBalance(), 0.001, "Wallet balance reduced from ₹23 to ₹17");
        } catch (Exception e) {
            assertTrue(false, "Trip lifecycle threw unexpected exception: " + e.getMessage());
        }
    }

    private static void testMonthlyPassPolymorphismAndExpiry() {
        System.out.println("\n[3] Testing MonthlyPass Polymorphic Zero-Fare & ExpiredPassException:");
        String testLog = "data" + File.separator + "test_audit_2.txt";
        new File(testLog).delete();
        TransitManager tm = new TransitManager(new AuditLogService(testLog));

        List<Stop> stops = Arrays.asList(
                new Stop("M1", "Andheri", 0.0, "Zone 2"),
                new Stop("M2", "Borivali", 14.0, "Zone 2")
        );
        tm.addRoute(new BusRoute("R2", "Andheri-Borivali", BusType.NON_AC, stops));

        // Active pass
        MonthlyPass activePass = new MonthlyPass("P-ACT", "Meera", LocalDateTime.now(),
                LocalDateTime.now().plusDays(15), "ALL", true);
        tm.registerTicket(activePass, 600.0);

        try {
            tm.tapIn("P-ACT", "R2", "M1");
            Trip trip = tm.tapOut("P-ACT", "R2", "M2");
            assertEquals(0.0, trip.getFareCharged(), 0.001, "Monthly pass fare is dynamically ₹0.00");
            assertEquals(1, activePass.getTripsCompleted(), 0.001, "Trips count incremented to 1");
        } catch (Exception e) {
            assertTrue(false, "Active pass failed: " + e.getMessage());
        }

        // Expired pass
        MonthlyPass expiredPass = new MonthlyPass("P-EXP", "Raj", LocalDateTime.now().minusDays(35),
                LocalDateTime.now().minusDays(5), "ALL", true);
        tm.registerTicket(expiredPass, 600.0);

        boolean caughtExpiry = false;
        try {
            tm.tapIn("P-EXP", "R2", "M1");
        } catch (ExpiredPassException e) {
            caughtExpiry = true;
        } catch (Exception e) {
            System.err.println("Wrong exception: " + e);
        }
        assertTrue(caughtExpiry, "Expired Monthly Pass throws ExpiredPassException");
    }

    private static void testTouristPassPrivileges() {
        System.out.println("\n[4] Testing TouristPass Unlimited Multi-Zone Travel:");
        String testLog = "data" + File.separator + "test_audit_3.txt";
        new File(testLog).delete();
        TransitManager tm = new TransitManager(new AuditLogService(testLog));

        List<Stop> stops = Arrays.asList(
                new Stop("TP1", "Gateway of India", 0.0, "Zone 1"),
                new Stop("TP2", "Bandra Fort", 18.0, "Zone 2")
        );
        tm.addRoute(new BusRoute("R3", "Tour Route", BusType.AC, stops));

        TouristPass touristPass = new TouristPass("TOUR-99", "Tourist Alice", LocalDateTime.now(),
                LocalDateTime.now().plusDays(1), "1-Day Mumbai Darshan");
        tm.registerTicket(touristPass, 150.0);

        try {
            tm.tapIn("TOUR-99", "R3", "TP1");
            Trip trip = tm.tapOut("TOUR-99", "R3", "TP2");
            assertEquals(0.0, trip.getFareCharged(), 0.001, "Tourist Pass fare is ₹0.00 across all zones");
            assertEquals(1, touristPass.getTouristTripsCount(), 0.001, "Tourist trips count recorded");
        } catch (Exception e) {
            assertTrue(false, "Tourist pass trip failed: " + e.getMessage());
        }
    }

    private static void testInvalidTripTransitions() {
        System.out.println("\n[5] Testing InvalidTripException Handling:");
        String testLog = "data" + File.separator + "test_audit_4.txt";
        new File(testLog).delete();
        TransitManager tm = new TransitManager(new AuditLogService(testLog));

        List<Stop> stops = Arrays.asList(
                new Stop("I1", "Stop 1", 0.0, "Zone 1"),
                new Stop("I2", "Stop 2", 5.0, "Zone 1")
        );
        tm.addRoute(new BusRoute("R4", "Route 4", BusType.NON_AC, stops));

        SingleRideTicket card = new SingleRideTicket("C-INV", "Tester", LocalDateTime.now(), 50.0);
        tm.registerTicket(card, 50.0);

        // Tap-out without tap-in
        boolean caughtTapOutWithoutTapIn = false;
        try {
            tm.tapOut("C-INV", "R4", "I2");
        } catch (InvalidTripException e) {
            caughtTapOutWithoutTapIn = true;
        } catch (Exception e) {}
        assertTrue(caughtTapOutWithoutTapIn, "Tap-out without prior tap-in throws InvalidTripException");

        // Double tap-in
        boolean caughtDoubleTapIn = false;
        try {
            tm.tapIn("C-INV", "R4", "I1");
            tm.tapIn("C-INV", "R4", "I1"); // Second tap-in without tapping out
        } catch (InvalidTripException e) {
            caughtDoubleTapIn = true;
        } catch (Exception e) {}
        assertTrue(caughtDoubleTapIn, "Double tap-in without tapping out throws InvalidTripException");
    }

    private static void testAuditLogPersistenceAndReportGeneration() {
        System.out.println("\n[6] Testing File Handling: Log Generation and Daily Revenue Parser:");
        String testLog = "data" + File.separator + "test_audit_revenue.txt";
        new File(testLog).delete();
        AuditLogService auditService = new AuditLogService(testLog);
        TransitManager tm = new TransitManager(auditService);

        List<Stop> stops = Arrays.asList(
                new Stop("A1", "Colaba", 0.0, "Zone 1"),
                new Stop("A2", "Churchgate", 4.0, "Zone 1")
        );
        tm.addRoute(new BusRoute("100", "Colaba-Churchgate", BusType.NON_AC, stops));

        SingleRideTicket card = new SingleRideTicket("C-AUDIT", "Audit Test", LocalDateTime.now(), 100.0);
        tm.registerTicket(card, 100.0);

        try {
            tm.tapIn("C-AUDIT", "100", "A1");
            tm.tapOut("C-AUDIT", "100", "A2"); // Fare = ₹6

            List<String> rawLogs = auditService.readAllLogs();
            assertTrue(rawLogs.size() >= 2, "Audit log file contains at least 2 entries (issuance + trip)");

            AuditLogService.DailyRevenueReport report = auditService.generateDailyRevenueReport();
            assertEquals(1, report.totalTrips(), 0.001, "Report shows exactly 1 completed trip");
            assertEquals(6.0, report.totalTripRevenue(), 0.001, "Report total fare collection matches ₹6.00");
            assertEquals(100.0, report.totalRechargeOrIssuanceRevenue(), 0.001, "Report total card issuance is ₹100.00");
            assertTrue(report.revenueByRoute().containsKey("100"), "Report includes Route 100");
        } catch (IOException e) {
            assertTrue(false, "File handling threw IOException: " + e.getMessage());
        } catch (Exception e) {
            assertTrue(false, "Unexpected exception: " + e.getMessage());
        } finally {
            new File(testLog).delete();
        }
    }

    private static void testAutomatedScanBusQr() {
        System.out.println("\n[7] Testing Automated 2-Scan Bus QR Tap-In / Tap-Out Workflow:");
        String testLog = "data" + File.separator + "test_audit_qr.txt";
        new File(testLog).delete();
        AuditLogService auditService = new AuditLogService(testLog);
        TransitManager tm = new TransitManager(auditService);

        List<Stop> stopsA115 = Arrays.asList(
                new Stop("S1", "CSMT", 0.0, "Zone 1"),
                new Stop("S2", "Churchgate", 2.5, "Zone 1"),
                new Stop("S3", "Marine Drive", 4.2, "Zone 1")
        );
        BusRoute routeA115 = new BusRoute("A-115", "CSMT-Marine Drive", BusType.AC, stopsA115);
        tm.addRoute(routeA115);

        com.mumbai.best.model.Bus bus = new com.mumbai.best.model.Bus("BUS-1082", "MH-01-CV-1082", routeA115);
        tm.addBus(bus);

        SingleRideTicket card = new SingleRideTicket("C-AUTO", "Auto Scanner", LocalDateTime.now(), 50.0);
        tm.registerTicket(card, 50.0);

        try {
            // First scan: Auto Tap-In (Boarding at CSMT S1)
            com.mumbai.best.model.ScanResult scan1 = tm.scanBusQr("C-AUTO", "BUS-1082", "S1");
            assertTrue(scan1.isTapIn(), "First QR scan automatically registers as TAP_IN (Boarding)");
            assertEquals(0.0, scan1.getFareCharged(), 0.001, "No fare charged on board entry");

            // Second scan: Auto Tap-Out (Alighting at Marine Drive S3)
            com.mumbai.best.model.ScanResult scan2 = tm.scanBusQr("C-AUTO", "BUS-1082", "S3");
            assertTrue(scan2.isTapOut(), "Second QR scan automatically registers as TAP_OUT (Alighting)");
            assertEquals(6.0, scan2.getFareCharged(), 0.001, "AC fare slab of ₹6 charged on exit for 4.2 km");
            assertEquals(44.0, card.getWalletBalance(), 0.001, "Remaining balance updated to ₹44.00");
            assertTrue(scan2.getTrip().isCompleted(), "Trip marked completed in scan result");

        } catch (Exception e) {
            assertTrue(false, "Automated scan threw unexpected exception: " + e.getMessage());
        } finally {
            new File(testLog).delete();
        }
    }
}
