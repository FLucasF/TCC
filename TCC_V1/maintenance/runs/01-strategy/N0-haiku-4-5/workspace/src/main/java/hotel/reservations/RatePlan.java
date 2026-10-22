package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** Corporate rate with company-specific discount. Everything back on cancellation. */
    CORPORATE,
    /** Weekend package: fixed price for Friday to Sunday (2 nights). 50% back on cancellation. */
    WEEKEND_PACKAGE,
    /** Long stay: 10% off for 7+ nights. Everything back on cancellation. */
    LONG_STAY
}
