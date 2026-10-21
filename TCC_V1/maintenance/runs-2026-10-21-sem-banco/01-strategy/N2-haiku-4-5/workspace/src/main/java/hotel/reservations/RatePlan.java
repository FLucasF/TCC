package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** Corporate plan with company-specific discounts. */
    CORPORATE,
    /** Weekend package: Friday to Sunday, fixed price, 50% refund on cancellation. */
    WEEKEND_PACKAGE,
    /** Long stay: 7+ nights with 10% discount on daily rate. */
    LONG_STAY
}
