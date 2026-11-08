package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Everything that is particular to a rate plan: who can use it, how the stay is priced, what comes back. */
interface RatePlanRule {

    /**
     * The price of the stay under this plan.
     *
     * @param standardPrice the stay at the room's rates, high season included, with no discount
     * @throws HotelException INVALID_REQUEST when the stay or the company code do not fit the plan
     */
    Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, Price standardPrice);

    /** What goes back to the guest who cancels after paying {@code amountPaid}. */
    BigDecimal refund(BigDecimal amountPaid);

    /** A discount on the sum of the nights, rounded to cents. */
    static Price withDiscount(Price standardPrice, BigDecimal percentage) {
        BigDecimal discount = PriceCalculator.cents(standardPrice.subtotal().multiply(percentage));
        return new Price(
                standardPrice.nights(), standardPrice.subtotal(), discount, standardPrice.subtotal().subtract(discount));
    }
}
