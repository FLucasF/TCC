package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** Corporate plan with company discounts. */
    CORPORATE,
    /** Weekend package: fixed price for 2 nights (Friday to Sunday). */
    WEEKEND_PACKAGE,
    /** Long stay discount: 10% off for 7 nights or more. */
    LONG_STAY
}
