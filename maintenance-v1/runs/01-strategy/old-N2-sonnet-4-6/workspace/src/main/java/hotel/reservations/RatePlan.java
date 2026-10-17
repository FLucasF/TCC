package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** Corporate discount by company code; full refund on cancellation. */
    CORPORATE,
    /** Fixed weekend package price (Fri–Sun); half refund on cancellation. */
    WEEKEND_PACKAGE,
    /** 7+ nights, 10% off; full refund on cancellation. */
    LONG_STAY
}
