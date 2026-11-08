package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** Corporate discount for partner companies; full refund on cancellation. */
    CORPORATE,
    /** Weekend package (Friday to Sunday) at fixed price; 50% refund on cancellation. */
    WEEKEND_PACKAGE,
    /** 10% off for stays of 7+ nights; full refund on cancellation. */
    LONG_STAY
}
