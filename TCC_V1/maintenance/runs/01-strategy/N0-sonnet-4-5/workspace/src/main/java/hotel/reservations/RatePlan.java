package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** Corporate discount based on company code; full refund on cancellation. */
    CORPORATE,
    /** Weekend package (Friday to Sunday, 2 nights) with fixed pricing; 50% refund on cancellation. */
    WEEKEND_PACKAGE,
    /** Long stay (7+ nights) with 10% discount; full refund on cancellation. */
    LONG_STAY
}
