package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** Corporate agreement: a discount based on the company code. Full refund on cancellation. */
    CORPORATE,
    /** Weekend package: Friday to Sunday (exactly 2 nights) at a fixed price. Half refund on cancellation. */
    WEEKEND_PACKAGE,
    /** Long stay: 7 nights or more with 10% discount. Full refund on cancellation. */
    LONG_STAY
}
