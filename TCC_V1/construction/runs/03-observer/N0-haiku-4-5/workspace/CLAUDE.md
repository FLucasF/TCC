# Development Guide - Pousada Maré Alta Reservation System

## Architecture Overview

The system is built with:
- **Java 21** with **Javalin 6.7.0** for REST API
- **PostgreSQL 18** for data persistence
- **Maven** for build and dependency management
- Modular design with notification system for future expansion

## Key Components

### Models (`src/main/java/com/pousadamarealta/models/`)
- `Room`: Represents a hotel room with type, capacity, and nightly rate
- `Reservation`: Complete reservation lifecycle and pricing data
- `Message`: Audit trail of notifications sent to staff
- `RoomType`: Enum for STANDARD, DELUXE, SUITE
- `RatePlan`: Enum for STANDARD and NON_REFUNDABLE
- `ReservationStatus`: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT or CANCELLED

### Services
- **ReservationService**: Core business logic for reservations
- **MessageService**: Manages notification storage
- **ReportService**: Generates occupancy and revenue CSV reports

### Utilities (`src/main/java/com/pousadamarealta/utils/`)
- **PriceCalculator**: Peak season detection, rate calculations with HALF_EVEN rounding
- **DateParser**: Standardized date parsing and formatting
- **PriceFormatter**: Currency formatting in Portuguese locale

### Notifications (`src/main/java/com/pousadamarealta/notifications/`)
- **NotificationManager**: Central hub for listener registration
- **NotificationListener**: Interface for pluggable notification handlers
- **GovernanceNotificationListener**: Alerts housekeeper staff
- **FinanceNotificationListener**: Alerts accounting department

## Adding New Notification Listeners

To add email alerts for restaurant or airport transfer:

1. Create `RestaurantNotificationListener implements NotificationListener`
2. Implement `onReservationConfirmed()` and `onReservationCancelled()` 
3. Register in `NotificationManager.static{}` block

Example:
```java
public class RestaurantNotificationListener implements NotificationListener {
    private static final String RESTAURANT_EMAIL = "restaurante@pousadamarealta.com.br";
    
    @Override
    public void onReservationConfirmed(Reservation reservation) {
        MessageService.sendMessage("EMAIL", RESTAURANT_EMAIL, 
            "Confirmação de café - " + DateParser.format(reservation.checkIn),
            "Hóspedes chegando em " + DateParser.format(reservation.checkIn));
    }
    
    @Override
    public void onReservationCancelled(Reservation reservation, BigDecimal refundAmount) {
        // Handle cancellations if needed
    }
}

// In NotificationManager static block:
register(new RestaurantNotificationListener());
```

## Business Rules

### Peak Season
- December 15 - February 28 (or 29)
- All of July
- Adds 20% to nightly rates

### Pricing
- All monetary amounts: BigDecimal with HALF_EVEN rounding to 2 decimal places
- Applied at each step: nightly rate, subtotal, discount, total
- Example: R$249.90 room on 07/10 → R$299.88 (1.20x rounding to nearest cent)

### Refunds
- STANDARD plan: 100% refund if cancelled before check-out
- NON_REFUNDABLE plan: 0% refund
- PENDING reservations: no refund (never paid)

### Room Constraints
- No overlapping reservations on same night (check_in counts, check_out doesn't)
- Guest count 1-N, must not exceed room capacity

## Testing

```bash
mvn test              # Run unit tests
mvn verify           # Full build validation
```

Unit tests for price calculation in `PriceCalculatorTest.java` validate:
- Peak season date detection
- 20% surcharge calculation
- 15% non-refundable discount
- Proper rounding to 2 decimal places

## Database Schema

Created on first run via `Database.initialize()`:
- `rooms`: 5 predefined rooms (101, 102, 201, 202, 301)
- `reservations`: Full reservation history with calculated prices
- `messages`: Audit log of all notifications sent

## Future Enhancements

### Guest Self-Service Features
- Check reservation status online
- Modify reservation dates
- Request document via email

### Advanced Reporting
- Occupancy forecasts
- Revenue per room type
- Cancellation statistics

### Integrations
- Real email sending (replace in-memory storage)
- SMS notifications
- Calendar exports (iCal)
- PMS integration

### Payment Processing
- Store payment method details
- Online payment processing
- Automatic confirmation workflow

## Environment Variables

- `DB_URL`: PostgreSQL connection string (required)
- `DB_USER`: Database user (required)
- `DB_PASSWORD`: Database password (required)
- `PORT`: HTTP server port (default: 7070)

## Error Handling

The API returns standardized error responses:
- `400 INVALID_REQUEST`: Missing/invalid fields
- `404 ROOM_NOT_FOUND`: Referenced room doesn't exist
- `404 RESERVATION_NOT_FOUND`: Referenced reservation doesn't exist
- `409 ROOM_UNAVAILABLE`: Room booked for requested dates
- `409 OPERATION_NOT_ALLOWED`: Invalid state transition
- `422 CAPACITY_EXCEEDED`: Too many guests for room
