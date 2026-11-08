package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** Corporate discount: company-specific discount, full refund on cancellation. */
    CORPORATE,
    /** Weekend package: Friday to Sunday, fixed price, 50% refund on cancellation. */
    WEEKEND_PACKAGE,
    /** Long stay (7+ nights): 10% discount on nights, full refund on cancellation. */
    LONG_STAY
}
