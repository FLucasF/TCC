package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** Corporate rate: discount depends on the company code. */
    CORPORATE,
    /** Weekend package: fixed price for two nights (Friday to Sunday), 50% refund on cancellation. */
    WEEKEND_PACKAGE,
    /** Long stay: 10% off for 7+ nights. */
    LONG_STAY
}
