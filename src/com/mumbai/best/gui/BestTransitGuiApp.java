package com.mumbai.best.gui;

import com.mumbai.best.exception.ExpiredPassException;
import com.mumbai.best.exception.InsufficientBalanceException;
import com.mumbai.best.exception.InvalidTripException;
import com.mumbai.best.exception.TransitException;
import com.mumbai.best.model.Bus;
import com.mumbai.best.model.BusRoute;
import com.mumbai.best.model.BusType;
import com.mumbai.best.model.MonthlyPass;
import com.mumbai.best.model.ScanResult;
import com.mumbai.best.model.SingleRideTicket;
import com.mumbai.best.model.Stop;
import com.mumbai.best.model.Ticket;
import com.mumbai.best.model.TouristPass;
import com.mumbai.best.model.Trip;
import com.mumbai.best.service.AuditLogService;
import com.mumbai.best.service.QrCodeGenerator;
import com.mumbai.best.service.TransitManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Visual Swing GUI application for the Mumbai BEST Bus Transit Network.
 * Features automated single-scan tap-in/tap-out, bus-tied QR codes,
 * GPS location approximation, live wallet management, and audit reporting.
 */
public class BestTransitGuiApp extends JFrame {

    private static final String DATA_DIR = "data";
    private static final String LOG_FILE = DATA_DIR + File.separator + "daily_transactions.txt";

    private final TransitManager transitManager;

    // GUI Components - Scanner Tab
    private JComboBox<BusItem> busSelector;
    private JLabel busQrImageLabel;
    private JLabel busInfoLabel;
    private JComboBox<TicketItem> cardSelector;
    private JComboBox<StopItem> gpsStopSelector;
    private JLabel commuterStatusLabel;
    private JTextArea scanResultArea;

    // GUI Components - Cards Tab
    private DefaultTableModel cardsTableModel;
    private JTable cardsTable;

    // GUI Components - Reports Tab
    private JTextArea reportTextArea;
    private JLabel reportSummaryLabel;

    public BestTransitGuiApp() {
        super("Mumbai BEST Bus Transit Management System • Smart QR Network");
        AuditLogService auditLogService = new AuditLogService(LOG_FILE);
        this.transitManager = new TransitManager(auditLogService);

        initializeDataAndBuses();
        setupLookAndFeel();
        initComponents();
        refreshAllViews();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 760);
        setMinimumSize(new Dimension(950, 680));
        setLocationRelativeTo(null);
    }

    private void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
    }

    private void initializeDataAndBuses() {
        // Route A-115: Electric AC Bus (South Mumbai Express)
        List<Stop> stopsA115 = Arrays.asList(
                new Stop("S101", "CSMT (Chhatrapati Shivaji Maharaj Terminus)", 0.0, "Zone 1 - Island City"),
                new Stop("S102", "Hutatma Chowk (Flora Fountain)", 1.2, "Zone 1 - Island City"),
                new Stop("S103", "Churchgate Station", 2.5, "Zone 1 - Island City"),
                new Stop("S104", "Marine Drive (Nariman Point)", 4.2, "Zone 1 - Island City"),
                new Stop("S105", "NCPA Arts Centre", 5.5, "Zone 1 - Island City"),
                new Stop("S106", "Cuffe Parade", 7.8, "Zone 1 - Island City"),
                new Stop("S107", "Backbay Bus Depot", 9.4, "Zone 1 - Island City")
        );
        BusRoute routeA115 = new BusRoute("A-115", "CSMT to Backbay Depot via Marine Drive", BusType.AC, stopsA115);
        transitManager.addRoute(routeA115);

        // Route 123: Regular Non-AC Bus (Colaba to Girgaon & Tardeo)
        List<Stop> stops123 = Arrays.asList(
                new Stop("S201", "R.C. Church (Colaba)", 0.0, "Zone 1 - Island City"),
                new Stop("S202", "Regal Cinema (Gateway of India)", 2.1, "Zone 1 - Island City"),
                new Stop("S203", "Churchgate Station", 4.5, "Zone 1 - Island City"),
                new Stop("S204", "Marine Lines", 6.0, "Zone 1 - Island City"),
                new Stop("S205", "Charni Road Station", 7.5, "Zone 1 - Island City"),
                new Stop("S206", "Girgaon Chowpatty", 9.0, "Zone 1 - Island City"),
                new Stop("S207", "Tardeo Central", 12.0, "Zone 1 - Island City")
        );
        BusRoute route123 = new BusRoute("123", "Colaba to Tardeo via Girgaon Chowpatty", BusType.NON_AC, stops123);
        transitManager.addRoute(route123);

        // Sample Physical Buses with dedicated QR codes
        Bus bus1 = new Bus("BUS-1082", "MH-01-CV-1082", routeA115);
        Bus bus2 = new Bus("BUS-4321", "MH-01-AP-4321", route123);
        transitManager.addBus(bus1);
        transitManager.addBus(bus2);

        // Generate and save QR code PNGs to data/
        QrCodeGenerator.saveSampleBusQrCodes(bus1, bus2, DATA_DIR);

        // Commuter Cards & Passes
        SingleRideTicket card1 = new SingleRideTicket("CARD-101", "Aarav Mehta", LocalDateTime.now().minusDays(5), 50.0);
        transitManager.registerTicket(card1, 50.0);

        SingleRideTicket card2 = new SingleRideTicket("CARD-102", "Sneha Patel (Low Balance)", LocalDateTime.now().minusDays(2), 4.0);
        transitManager.registerTicket(card2, 4.0);

        MonthlyPass pass1 = new MonthlyPass("PASS-201", "Rohan Deshmukh", LocalDateTime.now().minusDays(10),
                LocalDateTime.now().plusDays(20), "ALL", true);
        transitManager.registerTicket(pass1, 750.0);

        MonthlyPass passExpired = new MonthlyPass("PASS-202", "Vikram Rane (Expired)", LocalDateTime.now().minusDays(40),
                LocalDateTime.now().minusDays(2), "ALL", false);
        transitManager.registerTicket(passExpired, 500.0);

        TouristPass tour1 = new TouristPass("TOUR-301", "John Smith (Tourist)", LocalDateTime.now(),
                LocalDateTime.now().plusDays(1), "1-Day Mumbai Darshan Unlimited");
        transitManager.registerTicket(tour1, 150.0);
    }

    private void initComponents() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 13));

        tabbedPane.addTab("  📱 Scan Bus QR Code  ", createScannerTab());
        tabbedPane.addTab("  💳 Commuter Smart Cards  ", createCardsTab());
        tabbedPane.addTab("  📊 Daily Revenue & Audit Log  ", createReportsTab());
        tabbedPane.addTab("  🚏 Sample Buses & QR Posters  ", createSampleBusesTab());

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        add(createFooterPanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(24, 43, 73));
        header.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel title = new JLabel("BRIHANMUMBAI ELECTRIC SUPPLY & TRANSPORT (BEST)");
        title.setFont(new Font("SansSerif", Font.BOLD, 17));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Next-Gen Contactless Smart Card & QR Transit Management • Mumbai BEST Fares");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(new Color(200, 220, 245));

        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        return header;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(new Color(245, 245, 245));
        footer.setBorder(new EmptyBorder(4, 15, 4, 15));
        JLabel status = new JLabel("Mumbai BEST Smart Transit Engine Active • Persistence: " + LOG_FILE);
        status.setFont(new Font("SansSerif", Font.ITALIC, 11));
        status.setForeground(Color.DARK_GRAY);
        footer.add(status);
        return footer;
    }

    // ==========================================
    // TAB 1: SMART BUS QR SCANNER
    // ==========================================
    private JPanel createScannerTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top: Bus Selection Dropdown
        JPanel topBusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topBusPanel.setBorder(new TitledBorder("Select Physical BEST Bus"));
        topBusPanel.add(new JLabel("Active Bus in Service: "));

        busSelector = new JComboBox<>();
        busSelector.setFont(new Font("SansSerif", Font.BOLD, 12));
        for (Bus b : transitManager.getAllBuses()) {
            busSelector.addItem(new BusItem(b));
        }
        busSelector.addActionListener(e -> onBusChanged());
        topBusPanel.add(busSelector);
        panel.add(topBusPanel, BorderLayout.NORTH);

        // Center: Split view (Left = Visual Bus QR Code, Right = Commuter Scanner Controls)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.42);
        splitPane.setDividerLocation(420);

        // LEFT: Bus QR Code Display
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setBorder(new TitledBorder("Physical Bus QR Code (Affixed on Bus Entry/Exit)"));

        busQrImageLabel = new JLabel();
        busQrImageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        leftPanel.add(busQrImageLabel, BorderLayout.CENTER);

        busInfoLabel = new JLabel();
        busInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        busInfoLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        leftPanel.add(busInfoLabel, BorderLayout.SOUTH);

        splitPane.setLeftComponent(leftPanel);

        // RIGHT: Commuter Action & Tap-In/Tap-Out Flow
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBorder(new TitledBorder("Commuter Terminal • Scan & Ride"));

        JPanel controls = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // Commuter Card
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        controls.add(new JLabel("Commuter Card / Pass:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        cardSelector = new JComboBox<>();
        cardSelector.addActionListener(e -> updateCommuterStatusBadge());
        controls.add(cardSelector, gbc);

        // Card Status Pill
        gbc.gridx = 0; gbc.gridy = 1;
        controls.add(new JLabel("Trip Status:"), gbc);
        gbc.gridx = 1;
        commuterStatusLabel = new JLabel("Checking status...");
        commuterStatusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        controls.add(commuterStatusLabel, gbc);

        // GPS Location Dropdown
        gbc.gridx = 0; gbc.gridy = 2;
        controls.add(new JLabel("Current GPS Bus Stop:"), gbc);
        gbc.gridx = 1;
        gpsStopSelector = new JComboBox<>();
        controls.add(gpsStopSelector, gbc);

        // Big Scan QR Button
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JButton scanButton = new JButton("📷  SCAN BUS QR CODE (AUTOMATIC TAP-IN / TAP-OUT)");
        scanButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        scanButton.setBackground(new Color(16, 124, 65)); // Forest Transit Green
        scanButton.setForeground(Color.WHITE);
        scanButton.setFocusPainted(false);
        scanButton.setPreferredSize(new Dimension(280, 42));
        scanButton.addActionListener(e -> executeSmartQrScan());
        controls.add(scanButton, gbc);

        rightPanel.add(controls, BorderLayout.NORTH);

        // Bottom of Right Panel: Visual Receipt / Boarding Card
        JPanel resultPanel = new JPanel(new BorderLayout(5, 5));
        resultPanel.setBorder(new TitledBorder("Live Scan Feedback & Electronic Ticket Receipt"));
        scanResultArea = new JTextArea();
        scanResultArea.setEditable(false);
        scanResultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        scanResultArea.setBackground(new Color(248, 250, 252));
        resultPanel.add(new JScrollPane(scanResultArea), BorderLayout.CENTER);

        rightPanel.add(resultPanel, BorderLayout.CENTER);
        splitPane.setRightComponent(rightPanel);

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private void onBusChanged() {
        BusItem item = (BusItem) busSelector.getSelectedItem();
        if (item == null) return;
        Bus bus = item.bus;

        // Update Bus QR Image
        BufferedImage qrImg = QrCodeGenerator.generateBusQrImage(bus, 230);
        busQrImageLabel.setIcon(new ImageIcon(qrImg));

        busInfoLabel.setText(String.format("<html><center><b>Bus ID:</b> %s &nbsp;|&nbsp; <b>Plate:</b> %s<br>" +
                        "<b>Route %s:</b> %s (%s)</center></html>",
                bus.getBusId(), bus.getLicensePlate(), bus.getRoute().getRouteNumber(),
                bus.getRoute().getRouteName(), bus.getRoute().getBusType().getDisplayName()));

        // Update GPS Stops dropdown for this bus's route
        gpsStopSelector.removeAllItems();
        for (Stop s : bus.getRoute().getStops()) {
            gpsStopSelector.addItem(new StopItem(s));
        }

        updateCommuterStatusBadge();
    }

    private void updateCommuterStatusBadge() {
        TicketItem item = (TicketItem) cardSelector.getSelectedItem();
        if (item == null) return;
        Ticket ticket = item.ticket;

        Trip activeTrip = transitManager.getActiveTrips().get(ticket.getTicketId().toUpperCase());
        if (activeTrip == null) {
            commuterStatusLabel.setText("⚪ NOT ONBOARD • Ready to Board (Tap-In)");
            commuterStatusLabel.setForeground(new Color(20, 120, 20));
        } else {
            commuterStatusLabel.setText(String.format("🟢 ONBOARD Route %s at %s • Ready to Alight (Tap-Out)",
                    activeTrip.getRoute().getRouteNumber(), activeTrip.getBoardingStop().getName()));
            commuterStatusLabel.setForeground(new Color(0, 80, 180));
        }
    }

    /**
     * Executes the automatic tap-in or tap-out scan based on the passenger's current state.
     */
    private void executeSmartQrScan() {
        TicketItem ticketItem = (TicketItem) cardSelector.getSelectedItem();
        BusItem busItem = (BusItem) busSelector.getSelectedItem();
        StopItem stopItem = (StopItem) gpsStopSelector.getSelectedItem();

        if (ticketItem == null || busItem == null || stopItem == null) {
            JOptionPane.showMessageDialog(this, "Please select Bus, Commuter Card, and GPS Stop.",
                    "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Ticket ticket = ticketItem.ticket;
        Bus bus = busItem.bus;
        Stop stop = stopItem.stop;

        try {
            ScanResult result = transitManager.scanBusQr(ticket.getTicketId(), bus.getBusId(), stop.getStopId());

            if (result.isTapIn()) {
                // Successful Boarding (Tap-In)
                StringBuilder sb = new StringBuilder();
                sb.append("======================================================================\n");
                sb.append("                 ✅ TAP-IN SUCCESSFUL • WELCOME ABOARD                \n");
                sb.append("======================================================================\n");
                sb.append(String.format("PASSENGER:         %s (%s)\n", ticket.getCommuterName(), ticket.getTicketId()));
                sb.append(String.format("TICKET TYPE:       %s\n", ticket.getTicketType()));
                sb.append(String.format("BUS BOARDED:       %s [%s]\n", bus.getBusId(), bus.getLicensePlate()));
                sb.append(String.format("ROUTE:             Route %s (%s)\n", bus.getRoute().getRouteNumber(), bus.getRoute().getBusType().getDisplayName()));
                sb.append(String.format("BOARDING STOP:     %s (%.1f km, %s)\n", stop.getName(), stop.getDistanceFromOriginKm(), stop.getZone()));
                sb.append(String.format("BOARDING TIME:     %s\n", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss"))));
                if (ticket instanceof SingleRideTicket srt) {
                    sb.append(String.format("CURRENT BALANCE:   ₹%.2f (Min required ₹6.00 verified)\n", srt.getWalletBalance()));
                } else {
                    sb.append(String.format("PASS STATUS:       VERIFIED & VALID (Hop-On Hop-Off Active)\n"));
                }
                sb.append("----------------------------------------------------------------------\n");
                sb.append("👉 Next: When getting off, scan this bus QR code again to auto-exit!\n");
                sb.append("======================================================================\n");
                scanResultArea.setText(sb.toString());

            } else {
                // Successful Alighting (Tap-Out)
                Trip trip = result.getTrip();
                StringBuilder sb = new StringBuilder();
                sb.append("======================================================================\n");
                sb.append("                 ✅ TAP-OUT SUCCESSFUL • JOURNEY COMPLETED             \n");
                sb.append("======================================================================\n");
                sb.append(String.format("PASSENGER:         %s (%s)\n", ticket.getCommuterName(), ticket.getTicketId()));
                sb.append(String.format("BUS ALIGHTED:      %s [%s]\n", bus.getBusId(), bus.getLicensePlate()));
                sb.append(String.format("ROUTE:             Route %s (%s)\n", bus.getRoute().getRouteNumber(), bus.getRoute().getBusType().getDisplayName()));
                sb.append(String.format("JOURNEY:           %s  --->  %s\n", trip.getBoardingStop().getName(), trip.getAlightingStop().getName()));
                sb.append(String.format("DISTANCE TRAVELED: %.2f km\n", trip.getDistanceKm()));
                sb.append(String.format("BEST FARE DEDUCTED:₹%.2f\n", result.getFareCharged()));
                if (ticket instanceof SingleRideTicket srt) {
                    sb.append(String.format("REMAINING BALANCE: ₹%.2f\n", srt.getWalletBalance()));
                } else {
                    sb.append(String.format("PASS BENEFIT:      ₹0.00 FARE APPLIED (%s)\n", ticket.getTicketType()));
                }
                sb.append(String.format("LOG RECORD:        Appended to %s\n", LOG_FILE));
                sb.append("----------------------------------------------------------------------\n");
                sb.append("Thank you for traveling with BEST Mumbai! Have a great day.\n");
                sb.append("======================================================================\n");
                scanResultArea.setText(sb.toString());
            }

            refreshAllViews();

        } catch (InsufficientBalanceException ex) {
            String alert = "❌ TAP-IN DENIED • INSUFFICIENT BALANCE\n\n" +
                    ex.getMessage() + "\n\n" +
                    "Minimum ₹6.00 balance required to board BEST buses.\n" +
                    "Please recharge your card wallet in the 'Commuter Smart Cards' tab.";
            scanResultArea.setText(alert);
            JOptionPane.showMessageDialog(this, alert, "Insufficient Card Balance", JOptionPane.ERROR_MESSAGE);

        } catch (ExpiredPassException ex) {
            String alert = "❌ TAP-IN DENIED • EXPIRED COMMUTER PASS\n\n" +
                    ex.getMessage() + "\n\n" +
                    "Please renew your subscription to continue traveling.";
            scanResultArea.setText(alert);
            JOptionPane.showMessageDialog(this, alert, "Pass Expired", JOptionPane.ERROR_MESSAGE);

        } catch (InvalidTripException ex) {
            String alert = "❌ INVALID TRIP TRANSITION\n\n" + ex.getMessage();
            scanResultArea.setText(alert);
            JOptionPane.showMessageDialog(this, alert, "Trip Validation Error", JOptionPane.WARNING_MESSAGE);

        } catch (TransitException ex) {
            String alert = "❌ TRANSIT ERROR: " + ex.getMessage();
            scanResultArea.setText(alert);
            JOptionPane.showMessageDialog(this, alert, "Transit Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==========================================
    // TAB 2: SMART CARDS & WALLETS
    // ==========================================
    private JPanel createCardsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] columns = {"Card ID", "Commuter Name", "Classification", "Balance / Validity", "Trips", "Status"};
        cardsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        cardsTable = new JTable(cardsTableModel);
        cardsTable.setRowHeight(24);
        cardsTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cardsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        panel.add(new JScrollPane(cardsTable), BorderLayout.CENTER);

        // Buttons: Recharge Wallet & Issue New Card
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        JButton rechargeBtn = new JButton("💳  Recharge Card Wallet");
        rechargeBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        rechargeBtn.addActionListener(e -> handleRechargeDialog());
        btnPanel.add(rechargeBtn);

        JButton issueCardBtn = new JButton("➕  Issue New Smart Card / Pass");
        issueCardBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        issueCardBtn.setBackground(new Color(24, 43, 73));
        issueCardBtn.setForeground(Color.WHITE);
        issueCardBtn.addActionListener(e -> handleIssueCardDialog());
        btnPanel.add(issueCardBtn);

        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void handleRechargeDialog() {
        int row = cardsTable.getSelectedRow();
        String defaultId = row >= 0 ? (String) cardsTableModel.getValueAt(row, 0) : "CARD-102";

        String cardId = JOptionPane.showInputDialog(this, "Enter Smart Card ID to Recharge:", defaultId);
        if (cardId == null || cardId.trim().isEmpty()) return;

        String amountStr = JOptionPane.showInputDialog(this, "Enter Recharge Amount in Rupees (₹):", "50");
        if (amountStr == null || amountStr.trim().isEmpty()) return;

        try {
            double amount = Double.parseDouble(amountStr.trim());
            transitManager.rechargeCard(cardId.trim(), amount);
            JOptionPane.showMessageDialog(this, "Recharge of ₹" + amount + " successful for " + cardId + "!",
                    "Recharge Successful", JOptionPane.INFORMATION_MESSAGE);
            refreshAllViews();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid numeric amount.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (TransitException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Recharge Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleIssueCardDialog() {
        String[] options = {"1. Single Ride Smart Card (Wallet)", "2. Monthly Unlimited Commuter Pass", "3. Tourist Pass (Prepaid Darshan)"};
        int choice = JOptionPane.showOptionDialog(this, "Select Card Type to Issue:", "Issue New Ticket / Pass",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        if (choice < 0) return;

        String name = JOptionPane.showInputDialog(this, "Enter Commuter Full Name:", "Pooja Sharma");
        if (name == null || name.trim().isEmpty()) return;

        long idNum = (System.currentTimeMillis() % 9000) + 1000;
        if (choice == 0) {
            String amountStr = JOptionPane.showInputDialog(this, "Initial Wallet Recharge (₹):", "100");
            double amount = 100.0;
            try { amount = Double.parseDouble(amountStr); } catch (Exception ignored) {}
            SingleRideTicket ticket = new SingleRideTicket("CARD-" + idNum, name, LocalDateTime.now(), amount);
            transitManager.registerTicket(ticket, amount);
        } else if (choice == 1) {
            MonthlyPass pass = new MonthlyPass("PASS-" + idNum, name, LocalDateTime.now(),
                    LocalDateTime.now().plusMonths(1), "ALL", true);
            transitManager.registerTicket(pass, 750.0);
        } else {
            TouristPass tour = new TouristPass("TOUR-" + idNum, name, LocalDateTime.now(),
                    LocalDateTime.now().plusDays(1), "1-Day Mumbai Darshan");
            transitManager.registerTicket(tour, 150.0);
        }

        JOptionPane.showMessageDialog(this, "New Card issued successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        refreshAllViews();
    }

    // ==========================================
    // TAB 3: DAILY REVENUE & AUDIT REPORT
    // ==========================================
    private JPanel createReportsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel topBar = new JPanel(new BorderLayout());
        reportSummaryLabel = new JLabel("Audit File: " + LOG_FILE);
        reportSummaryLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        topBar.add(reportSummaryLabel, BorderLayout.WEST);

        JButton refreshBtn = new JButton("🔄  Refresh Report from Audit File");
        refreshBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        refreshBtn.addActionListener(e -> refreshReportsView());
        topBar.add(refreshBtn, BorderLayout.EAST);
        panel.add(topBar, BorderLayout.NORTH);

        reportTextArea = new JTextArea();
        reportTextArea.setEditable(false);
        reportTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        reportTextArea.setBackground(new Color(250, 252, 255));
        panel.add(new JScrollPane(reportTextArea), BorderLayout.CENTER);

        return panel;
    }

    private void refreshReportsView() {
        try {
            AuditLogService.DailyRevenueReport report = transitManager.getAuditLogService().generateDailyRevenueReport();
            reportTextArea.setText(report.formatReport());
            reportSummaryLabel.setText(String.format("Audit File: %s | Total Inflow: ₹%.2f | Total Rides: %d",
                    LOG_FILE, report.totalTripRevenue() + report.totalRechargeOrIssuanceRevenue(), report.totalTrips()));
        } catch (IOException e) {
            reportTextArea.setText("Error reading audit log file: " + e.getMessage());
        }
    }

    // ==========================================
    // TAB 4: SAMPLE BUSES & QR POSTERS
    // ==========================================
    private JPanel createSampleBusesTab() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        List<Bus> buses = transitManager.getAllBuses();
        for (Bus b : buses) {
            JPanel busCard = new JPanel(new BorderLayout(10, 10));
            busCard.setBorder(new LineBorder(new Color(200, 210, 225), 2, true));
            busCard.setBackground(Color.WHITE);

            JLabel busTitle = new JLabel(String.format("<html><center><h3>%s</h3><b>Plate: %s</b><br>Route %s (%s)</center></html>",
                    b.getBusId(), b.getLicensePlate(), b.getRoute().getRouteNumber(), b.getRoute().getBusType().getDisplayName()));
            busTitle.setHorizontalAlignment(SwingConstants.CENTER);
            busCard.add(busTitle, BorderLayout.NORTH);

            BufferedImage qr = QrCodeGenerator.generateBusQrImage(b, 220);
            JLabel qrLabel = new JLabel(new ImageIcon(qr));
            qrLabel.setHorizontalAlignment(SwingConstants.CENTER);
            busCard.add(qrLabel, BorderLayout.CENTER);

            JLabel desc = new JLabel(String.format("<html><center>Affixed inside bus cabin.<br>Saved at: <code>data/qr_%s.png</code></center></html>",
                    b.getBusId().toLowerCase()));
            desc.setHorizontalAlignment(SwingConstants.CENTER);
            desc.setFont(new Font("SansSerif", Font.PLAIN, 11));
            busCard.add(desc, BorderLayout.SOUTH);

            panel.add(busCard);
        }

        return panel;
    }

    private void refreshAllViews() {
        // Refresh Card Selector in Scanner
        Object selected = cardSelector.getSelectedItem();
        cardSelector.removeAllItems();
        for (Ticket t : transitManager.getAllTickets()) {
            cardSelector.addItem(new TicketItem(t));
        }
        if (selected != null) {
            for (int i = 0; i < cardSelector.getItemCount(); i++) {
                if (cardSelector.getItemAt(i).ticket.getTicketId().equals(((TicketItem) selected).ticket.getTicketId())) {
                    cardSelector.setSelectedIndex(i);
                    break;
                }
            }
        }

        // Refresh Cards Table
        cardsTableModel.setRowCount(0);
        for (Ticket t : transitManager.getAllTickets()) {
            String val = "-";
            int trips = 0;
            String status = "Active";

            if (t instanceof SingleRideTicket srt) {
                val = String.format("₹%.2f", srt.getWalletBalance());
                if (srt.getWalletBalance() < 6.0) status = "Low Balance (< ₹6)";
            } else if (t instanceof MonthlyPass mp) {
                val = "Valid till " + mp.getValidUntil().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
                trips = mp.getTripsCompleted();
                if (LocalDateTime.now().isAfter(mp.getValidUntil())) status = "EXPIRED";
            } else if (t instanceof TouristPass tp) {
                val = "Valid till " + tp.getValidUntil().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
                trips = tp.getTouristTripsCount();
                if (LocalDateTime.now().isAfter(tp.getValidUntil())) status = "EXPIRED";
            }

            cardsTableModel.addRow(new Object[]{
                    t.getTicketId(),
                    t.getCommuterName(),
                    t.getTicketType(),
                    val,
                    trips,
                    status
            });
        }

        onBusChanged();
        refreshReportsView();
    }

    public static void main(String[] args) {
        if (args.length > 0 && ("--demo".equalsIgnoreCase(args[0]) || "--cli".equalsIgnoreCase(args[0]))) {
            com.mumbai.best.app.BestTransitApp.main(args);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            BestTransitGuiApp app = new BestTransitGuiApp();
            app.setVisible(true);
        });
    }

    // Helper combo wrapper classes
    private record BusItem(Bus bus) {
        @Override
        public String toString() {
            return String.format("%s (%s) • Route %s %s",
                    bus.getBusId(), bus.getLicensePlate(), bus.getRoute().getRouteNumber(), bus.getRoute().getBusType().getDisplayName());
        }
    }

    private record TicketItem(Ticket ticket) {
        @Override
        public String toString() {
            if (ticket instanceof SingleRideTicket srt) {
                return String.format("%s - %s [Wallet: ₹%.2f]", ticket.getTicketId(), ticket.getCommuterName(), srt.getWalletBalance());
            } else {
                return String.format("%s - %s [%s]", ticket.getTicketId(), ticket.getCommuterName(), ticket.getTicketType());
            }
        }
    }

    private record StopItem(Stop stop) {
        @Override
        public String toString() {
            return String.format("%s (%.1f km) [%s]", stop.getName(), stop.getDistanceFromOriginKm(), stop.getZone());
        }
    }
}
