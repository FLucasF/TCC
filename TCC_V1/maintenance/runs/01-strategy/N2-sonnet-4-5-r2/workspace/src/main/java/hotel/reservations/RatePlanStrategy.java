package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Each rate plan has its own rules for validation, pricing, and refunds. A strategy packages
 * those three decisions so the next plan can enter in one place.
 */
public interface RatePlanStrategy {

    /**
     * Checks whether this stay is allowed under this plan. Throws HotelException (INVALID_REQUEST)
     * if the rules are not met.
     */
    void validate(Room room, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode);

    /** Calculates the price for this stay under this plan. */
    Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator calculator);

    /** How much of what was paid comes back when the reservation is cancelled. */
    BigDecimal calculateRefund(BigDecimal amountPaid);
}
