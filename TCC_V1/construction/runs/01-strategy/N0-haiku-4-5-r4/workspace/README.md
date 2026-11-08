# Pousada Maré Alta - Reservation System

A complete hotel reservation management system for Pousada Maré Alta, built with Java, Javalin, and PostgreSQL.

## Building

```bash
mvn clean package
java -jar target/pousada.jar
```

## Configuration

The system requires PostgreSQL database and the following environment variables:

- `DB_URL`: PostgreSQL connection URL (e.g., `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Database user
- `DB_PASSWORD`: Database password
- `PORT`: Server port (default: 7070)

## Features

### Rooms
- 2 Standard rooms (101, 102) - 2 guests, R$ 249.90/night
- 2 Deluxe rooms (201, 202) - 3 guests, R$ 389.90/night
- 1 Suite (301) - 4 guests, R$ 579.90/night

### Rate Plans
1. **STANDARD** - Normal pricing with full refund on cancellation
2. **NON_REFUNDABLE** - 15% discount, no refund on cancellation
3. **CORPORATE** - Company discounts (TECNOSUL 12%, AGROVALE 18%, FARMAVIDA 25%), full refund
4. **WEEKEND_PACKAGE** - Friday-Sunday only, fixed prices (R$ 520/820/1200), 50% refund
5. **LONG_STAY** - 7+ nights, 10% discount, full refund

### Pricing
- High season (Dec 15-Feb 28, July): +20% per night
- Banker's rounding (half to even) at each calculation step
- Discounts applied to subtotal, not individual nights

### API Endpoints

**Rooms**
- `GET /rooms` - List all rooms

**Reservations**
- `POST /reservations` - Create new reservation
- `GET /reservations/{id}` - Get reservation details
- `POST /reservations/{id}/confirmation` - Confirm payment
- `POST /reservations/{id}/cancellation` - Cancel reservation
- `POST /reservations/{id}/check-in` - Check in guest
- `POST /reservations/{id}/check-out` - Check out guest
- `GET /reservations/{id}/document` - Get voucher copy

**Messages**
- `GET /messages` - List sent emails

**Reports**
- `GET /reports/occupancy?date=2027-07-10` - Occupancy CSV for date
- `GET /reports/revenue?month=2027-07` - Revenue CSV for month

## Example Usage

### Create a reservation
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Confirm reservation
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 779.80}'
```

### Check in
```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

### Check out
```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

## Testing

Run tests:
```bash
mvn verify
```

## Database

The system automatically creates all required tables on startup. No manual schema setup needed.

Tables:
- `rooms` - Room master data
- `reservations` - Reservation records
- `messages` - Email/message logs
