package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** Discount per company code; full refund on cancellation. */
    CORPORATE,
    /** Fixed price for Friday to Sunday (2 nights only); 50% refund on cancellation. */
    WEEKEND_PACKAGE,
    /** 10% off for 7+ nights; full refund on cancellation. */
    LONG_STAY
}
