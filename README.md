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


