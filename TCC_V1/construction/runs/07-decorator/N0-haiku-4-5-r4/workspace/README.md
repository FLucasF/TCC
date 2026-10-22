# Pousada Maré Alta - Reservation System

A complete reservation management system for Pousada Maré Alta, built with Java 21, Javalin, and PostgreSQL.

## Features

### Room Management
- 5 pre-configured rooms with different types and pricing
- STANDARD (101-102): 2 guests, R$ 249,90/night
- DELUXE (201-202): 3 guests, R$ 389,90/night
- SUITE (301): 4 guests, R$ 579,90/night

### Pricing
- High season surcharge (20%): Dec 15 - Feb 28 and July
- Non-refundable rate plan with 15% discount
- Banker's rounding for all monetary calculations

### Reservations
Complete lifecycle management:
- PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
- Cancellation with refund rules based on rate plan
- Automatic conflict detection (no double-bookings)

### Email System
- Email validation (requires @ and domain with dot)
- Test mode (HOTEL_TEST_MODE=true) redirects to test email
- Automatic archive copies for audit trail
- Blocked messages for invalid email addresses

### Reporting
- Occupancy reports (CSV) showing room usage by night
- Revenue reports (CSV) showing financial data by month

## Building

```bash
mvn clean package
```

This creates an executable JAR in `target/reservations-1.0.0.jar`.

## Running

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="password"
export PORT=7070
export HOTEL_TEST_MODE=false

java -jar target/reservations-1.0.0.jar
```

### Environment Variables
- `DB_URL`: PostgreSQL connection URL (required)
- `DB_USER`: Database user (required)
- `DB_PASSWORD`: Database password (required)
- `PORT`: HTTP port (default: 7070)
- `HOTEL_TEST_MODE`: Enable test mode for emails (true/false, default: false)

## API Endpoints

### Rooms
- `GET /rooms` - List all rooms

### Reservations
- `POST /reservations` - Create a new reservation
- `GET /reservations/{id}` - Get reservation details
- `POST /reservations/{id}/confirmation` - Confirm payment
- `POST /reservations/{id}/check-in` - Check in guest
- `POST /reservations/{id}/check-out` - Check out guest
- `POST /reservations/{id}/cancellation` - Cancel reservation
- `GET /reservations/{id}/document` - Get receipt copy

### Messages
- `GET /messages` - List all sent/blocked messages

### Reports
- `GET /reports/occupancy?date=YYYY-MM-DD` - Room occupancy for a specific night
- `GET /reports/revenue?month=YYYY-MM` - Revenue report for a specific month

## Testing

```bash
mvn verify
```

## Database Schema

The system automatically creates the following tables on startup:
- `rooms` - Room definitions
- `reservations` - Reservation records
- `messages` - Email message log

## Architecture

- **Application.java**: Main entry point
- **database/**: Database connection and initialization
- **model/**: Data models (Room, Reservation, Message)
- **repository/**: Database access layer
- **service/**: Business logic (pricing, messaging)
- **api/**: REST endpoints

## Notes

- All monetary values use 2 decimal places with banker's rounding
- Archive emails are sent to `arquivo@pousadamarealta.com.br`
- Test mode emails go to `testes@pousadamarealta.com.br`
- Database is created automatically on first run
