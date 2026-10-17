package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** Corporate discount based on company code. */
    CORPORATE,
    /** Weekend package: Friday to Sunday, fixed price, 50% refund on cancellation. */
    WEEKEND_PACKAGE,
    /** Long stay discount for 7+ nights, 100% refund on cancellation. */
    LONG_STAY
}
