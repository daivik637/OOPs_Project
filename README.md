# Mumbai BEST Bus Transit Management System (Java OOP)

A complete, production-grade Java application modeling the **Mumbai BEST (Brihanmumbai Electric Supply and Transport) Transit Network**. It features a modern **Visual GUI Application (Swing Desktop)**, automated QR code boarding and alighting, bus-tied QR codes, simulated GPS stop detection, dynamic BEST fare deduction, file-based audit logging, and daily revenue reporting.

---

## 🚌 Real-World Problem & Automated QR Workflow
In Mumbai's high-density urban transit network, commuters board and alight from BEST buses frequently. To modernize commuter flow and fare collection:
1. **Bus-Tied QR Codes**: Every physical bus carries its own unique QR code affixed inside the vehicle (e.g. `BUS-1082` on Route A-115 Electric AC, and `BUS-4321` on Route 123 Regular Non-AC).
2. **Automated Single-Click Scanning**:
   - Commuters do **not** need to manually select whether they are getting on or getting off.
   - **First Scan (Tap-In)**: Scanning the bus QR code automatically detects that the commuter is not on board, verifies minimum required balance (₹6.00) or pass validity, detects boarding stop via simulated GPS location, and registers the in-flight trip.
   - **Second Scan (Tap-Out)**: Scanning the bus QR code again upon exiting automatically detects they are on board, records alighting stop via GPS, calculates distance traveled, computes the dynamic BEST fare, and deducts the fare from their wallet (or validates ₹0 for monthly/tourist passes).
3. **Audit & Revenue Intelligence**: Every transaction (recharge, trip completion, tap-in infraction) is permanently written to a daily audit text file (`data/daily_transactions.txt`), which is parsed to generate end-of-day revenue reports.

---

## 🏛️ Key Object-Oriented Programming (OOP) Features

### 1. Inheritance
- **Base Class**: `Ticket` (`com.mumbai.best.model.Ticket`)
  - Common attributes: `ticketId`, `commuterName`, `issuedAt`.
  - Abstract contracts: `calculateFare(...)`, `validateTapIn(...)`, `processTapOut(...)`, `getTicketType()`, `getDetails()`.
- **Derived Classes**:
  - `SingleRideTicket`: Smart card wallet with balance tracking, recharge capability, and per-ride fare deductions.
  - `MonthlyPass`: Subscribed commuter pass with validity timestamp (`validUntil`), zone scope (`ALL` or specific zone), and AC permissions.
  - `TouristPass`: Hop-on hop-off 1-Day or multi-day unlimited BEST city travel pass for tourists across all zones and bus categories.

### 2. Polymorphism
- **Dynamic Fare Calculation**:
  - `calculateFare(BusRoute route, Stop boardingStop, Stop alightingStop)` is overridden dynamically across ticket subclasses:
    - `SingleRideTicket`: Computes distance-based fare using Mumbai BEST slabs + zone consideration.
    - `MonthlyPass`: Overrides fare to **₹0.00** for covered routes/zones (or calculates supplemental out-of-zone / AC upgrade fees).
    - `TouristPass`: Overrides fare to **₹0.00** for all routes, stops, and zones.
- **Dynamic Tap-In & Tap-Out Processing**:
  - `validateTapIn()` dynamically checks wallet balance for single-ride cards versus expiration dates for passes.
  - `processTapOut()` dynamically deducts money from wallet or records pass usage counts.

### 3. File Handling (I/O)
- **Log Generation**:
  - Every event (card issuance, wallet recharge, completed trip, failed attempt) is appended to `data/daily_transactions.txt` via `BufferedWriter` and `PrintWriter` with ISO timestamps and structured pipe-delimited records.
- **Log Reading & Daily Revenue Reporting**:
  - `AuditLogService.generateDailyRevenueReport()` reads the audit log file line-by-line using `BufferedReader`, parses fields, aggregates route-wise earnings, category breakdowns (AC vs Non-AC), ticket distributions, and outputs a formatted **Daily Revenue & Audit Summary Report**.

### 4. Custom Exception Handling
- `TransitException`: Base checked transit exception.
- `InsufficientBalanceException`: Thrown when wallet balance is lower than the required minimum (₹6.00) on tap-in or lower than the computed journey fare on tap-out. Encapsulates `currentBalance` and `requiredAmount`.
- `ExpiredPassException`: Thrown when a monthly pass or tourist pass has expired. Encapsulates `expiryTime`.
- `InvalidTripException`: Thrown during invalid state transitions (e.g. attempting to board a second bus without alighting from the first, or tap-out without tap-in).

---

## 💰 Mumbai BEST Bus Fare Slabs (Implemented)

| Distance Traveled | Regular Non-AC Fare | Cool AC Fare |
|-------------------|---------------------|--------------|
| **0 to 5.0 km**   | ₹6.00               | ₹6.00        |
| **5.1 to 10.0 km**| ₹10.00              | ₹13.00       |
| **10.1 to 15.0 km**| ₹15.00             | ₹19.00       |
| **15.1 to 20.0 km**| ₹20.00             | ₹25.00       |
| **20.1 to 25.0 km**| ₹25.00             | ₹31.00       |
| **25.1 to 30.0 km**| ₹30.00             | ₹37.00       |
| **Beyond 30.0 km**| ₹35.00              | ₹45.00       |

---

## 🖥️ Graphical User Interface (Java Swing)

The visual GUI application provides:
1. **📱 Scan Bus QR Code**:
   - Live visual QR Code for the selected physical bus (`BUS-1082` AC or `BUS-4321` Non-AC).
   - Commuter Card Selector with current balance/status pill.
   - GPS Location Dropdown (simulates bus position along route).
   - Single **"Scan Bus QR Code"** button (automates tap-in on 1st scan and tap-out on 2nd scan).
   - Instant digital boarding pass or alighting receipt with fare breakdown.
2. **💳 Commuter Smart Cards**:
   - Interactive table of cards with balances, validities, and statuses.
   - One-click wallet recharge dialog.
   - Modal for issuing new Single Ride, Monthly, or Tourist Passes.
3. **📊 Daily Revenue & Audit Log**:
   - Visual summary of gross daily inflow, total trips, and infraction alerts.
   - Live parsed report and transaction log stream read directly from `data/daily_transactions.txt`.
4. **🚏 Sample Buses & QR Posters**:
   - Visual posters showing both sample buses with their dedicated scannable QR codes and route summaries.

---

## 📂 Project Structure

```
OOPs project/
├── bin/                               # Compiled Java bytecode
├── data/
│   ├── daily_transactions.txt         # Audit log file generated and read by File Handling
│   ├── qr_bus-1082.png                # Generated 2D QR Code poster for Sample Bus 1 (AC)
│   └── qr_bus-4321.png                # Generated 2D QR Code poster for Sample Bus 2 (Non-AC)
├── src/
│   └── com/
│       └── mumbai/
│           └── best/
│               ├── app/
│               │   └── BestTransitApp.java          # CLI application & automated demo runner
│               ├── gui/
│               │   └── BestTransitGuiApp.java       # Visual Java Swing Desktop Application
│               ├── exception/
│               │   ├── TransitException.java        # Base checked exception
│               │   ├── InsufficientBalanceException.java
│               │   ├── ExpiredPassException.java
│               │   └── InvalidTripException.java
│               ├── model/
│               │   ├── Bus.java                     # Physical bus entity with unique QR data
│               │   ├── BusType.java                 # Enum (NON_AC, AC)
│               │   ├── Stop.java                    # Bus stop entity (name, km, zone)
│               │   ├── BusRoute.java                # Route entity with ordered stops
│               │   ├── Trip.java                    # In-flight & completed trip model
│               │   ├── ScanResult.java              # Tap-in/Tap-out scan outcome
│               │   ├── Ticket.java                  # Abstract base class (Inheritance)
│               │   ├── SingleRideTicket.java        # Wallet card subclass
│               │   ├── MonthlyPass.java             # Monthly pass subclass
│               │   └── TouristPass.java             # Tourist pass subclass
│               ├── service/
│               │   ├── FareCalculator.java          # Official BEST fare slabs logic
│               │   ├── AuditLogService.java         # Text file read/write & report generator
│               │   ├── QrCodeGenerator.java         # 2D QR Code image generation & saving
│               │   └── TransitManager.java          # Transit controller & automated 2-scan engine
│               └── test/
│                   └── TransitSystemTest.java       # Complete 35-case automated test suite
├── run.bat                            # Windows batch launcher (runs Visual GUI by default)
├── run.ps1                            # PowerShell launcher (runs Visual GUI by default)
├── BestTransitApp.exe                 # Native Windows executable (double-click to launch)
├── BestTransitApp.jar                 # Standalone runnable JAR file
├── dist/
│   └── BestTransitStandalone/         # Fully portable standalone application (contains embedded JRE)
│       └── BestTransitStandalone.exe  # Standalone executable (runs on any Windows PC without Java)
├── UML_Class_Diagram.pdf              # Print-ready landscape UML diagram PDF
├── UML_CLASS_DIAGRAM.md               # Complete UML Class Diagram & architecture specification
└── README.md
```

---

## 🚀 How to Run the Executable

### 1. Native Windows Executable (Recommended)
Simply **double-click** [`BestTransitApp.exe`](file:///c:/Users/Daivik/OneDrive/Documents/OOPs%20project/BestTransitApp.exe) in your project folder.
It immediately launches the Visual Desktop GUI without opening any command-line window!

Or run from terminal:
```powershell
.\BestTransitApp.exe
```

### 2. Standalone Portable Package (Zero Java Required)
In the [`dist/BestTransitStandalone`](file:///c:/Users/Daivik/OneDrive/Documents/OOPs%20project/dist/BestTransitStandalone/) directory, double-click:
`BestTransitStandalone.exe`
This contains its own private embedded Java runtime and can be zipped or copied to any Windows computer without installing Java.

### 3. Executable Runnable JAR
Double-click [`BestTransitApp.jar`](file:///c:/Users/Daivik/OneDrive/Documents/OOPs%20project/BestTransitApp.jar) or run:
```powershell
java -jar BestTransitApp.jar
```

### 4. Run Unit Test Suite (35 Automated Tests)
```powershell
java "-Dfile.encoding=UTF-8" -cp bin com.mumbai.best.test.TransitSystemTest
```

### 5. Run Automated CLI Demo
```powershell
.\BestTransitApp.exe --demo
```
