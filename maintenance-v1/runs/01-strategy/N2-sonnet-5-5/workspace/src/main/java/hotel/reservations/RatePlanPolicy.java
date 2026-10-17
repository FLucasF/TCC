package hotel.reservations;

import java.math.BigDecimal;

/** What makes a rate plan different: who may use it, how the stay is priced and what a cancellation returns. */
public interface RatePlanPolicy {

    /**
     * The price of the stay under this plan.
     *
     * @param fullPrice the stay at the room's nightly rates, high season included, with no discount
     * @throws HotelException INVALID_REQUEST when the stay cannot use this plan
     */
    Price price(Stay stay, Price fullPrice);

    /** What comes back to the guest who cancels after paying {@code amountPaid}. */
    BigDecimal refund(BigDecimal amountPaid);
}
