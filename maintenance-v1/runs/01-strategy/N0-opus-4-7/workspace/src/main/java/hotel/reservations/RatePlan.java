package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** Corporate agreement: a known company code gets its own discount; full refund on cancellation. */
    CORPORATE,
    /** Friday to Sunday, exactly two nights, at a flat price per room type; half comes back on cancellation. */
    WEEKEND_PACKAGE,
    /** Seven nights or more: 10% off the nightly subtotal; full refund on cancellation. */
    LONG_STAY
}
