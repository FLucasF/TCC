package hotel.reservations.plans;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/** The pricing primitives a plan policy uses to turn a stay into money. */
public interface PricingSupport {

    int nights(LocalDate checkIn, LocalDate checkOut);

    /** The sum of the nightly prices (with the high-season surcharge on high-season nights). */
    BigDecimal subtotal(Room room, LocalDate checkIn, LocalDate checkOut);

    /** Rounds money to cents, half-even, the same way the rest of the system does. */
    BigDecimal cents(BigDecimal value);
}
