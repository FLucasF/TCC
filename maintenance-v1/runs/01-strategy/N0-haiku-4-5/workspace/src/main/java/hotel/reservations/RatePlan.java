package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** Corporate rate: discount varies by company; full refund on cancellation. */
    CORPORATE,
    /** Weekend package: fixed price for Friday to Sunday; 50% refund on cancellation. */
    WEEKEND_PACKAGE,
    /** Long stay: 10% off for stays of 7+ nights; full refund on cancellation. */
    LONG_STAY
}
