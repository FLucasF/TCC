package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** Corporate rate with company-specific discounts. Company code required. Full refund on cancellation. */
    CORPORATE,
    /** Weekend package: fixed price for Friday to Sunday. 50% refund on cancellation. */
    WEEKEND_PACKAGE,
    /** Long stay: 10% discount for 7+ nights. Full refund on cancellation. */
    LONG_STAY
}
