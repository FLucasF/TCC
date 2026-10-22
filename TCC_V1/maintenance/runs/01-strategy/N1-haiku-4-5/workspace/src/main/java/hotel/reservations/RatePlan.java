package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** Corporate plan with company-based discounts: TECNOSUL (12%), AGROVALE (18%), FARMAVIDA (25%). */
    CORPORATE,
    /** Fixed-price weekend package: Friday to Sunday, two nights. 50% refund on cancellation. */
    WEEKEND_PACKAGE,
    /** Long stay: 7+ nights with 10% discount. Full refund on cancellation. */
    LONG_STAY
}
