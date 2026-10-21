package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** Corporate plan with discounts per company code; everything comes back on cancellation. */
    CORPORATE,
    /** Fixed-price weekend package (Friday to Sunday, exactly 2 nights); 50% comes back on cancellation. */
    WEEKEND_PACKAGE,
    /** 7+ nights with 10% discount; everything comes back on cancellation. */
    LONG_STAY
}
