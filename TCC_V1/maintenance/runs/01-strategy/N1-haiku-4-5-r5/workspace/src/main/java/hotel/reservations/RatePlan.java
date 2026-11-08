package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** Corporate plan with company discounts; guest gets everything back on cancellation. */
    CORPORATE,
    /** Weekend package: fixed price for Friday to Sunday; 50% back on cancellation. */
    WEEKEND_PACKAGE,
    /** Long stay: 7+ nights with 10% discount; guest gets everything back on cancellation. */
    LONG_STAY
}
