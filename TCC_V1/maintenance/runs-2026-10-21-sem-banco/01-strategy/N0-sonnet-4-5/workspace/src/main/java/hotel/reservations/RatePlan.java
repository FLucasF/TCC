package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** Corporate discount based on company code; full refund on cancellation. */
    CORPORATE,
    /** Fixed weekend package price (Friday to Sunday); 50% refund on cancellation. */
    WEEKEND_PACKAGE,
    /** 10% discount for stays of 7 nights or more; full refund on cancellation. */
    LONG_STAY
}
