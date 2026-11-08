package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** Corporate discount for partner companies; guest gets everything back on cancellation. */
    CORPORATE,
    /** Fixed-price weekend package (Friday check-in, Sunday check-out); guest gets 50% back on cancellation. */
    WEEKEND_PACKAGE,
    /** Discount for stays of 7+ nights; guest gets everything back on cancellation. */
    LONG_STAY
}
