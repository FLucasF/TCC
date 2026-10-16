package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Everything that sets one rate plan apart from the others: who may book it, how its stay is priced,
 * and what comes back when it is cancelled before the stay. A new plan is a new implementation of this
 * interface wired to a {@link RatePlan} constant; nothing else in the system needs to change.
 */
public interface RatePlanPolicy {

    /**
     * Rejects, with {@link ErrorCode#INVALID_REQUEST}, a stay this plan cannot take (the wrong dates, a
     * missing or unknown company code, ...). By default a plan takes any stay.
     */
    default void validate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {}

    /** The price of the stay under this plan. */
    Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator pricing);

    /** What a guest who paid {@code amountPaid} gets back when cancelling before the stay starts. */
    BigDecimal refund(BigDecimal amountPaid);
}
