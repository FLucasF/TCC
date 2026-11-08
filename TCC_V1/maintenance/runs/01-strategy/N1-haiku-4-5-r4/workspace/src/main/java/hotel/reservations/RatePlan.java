package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** Corporate rate with company-specific discounts. */
    CORPORATE,
    /** Weekend package: Friday to Sunday for a fixed price. */
    WEEKEND_PACKAGE,
    /** Long stay discount: 7+ nights get 10% off. */
    LONG_STAY
}
