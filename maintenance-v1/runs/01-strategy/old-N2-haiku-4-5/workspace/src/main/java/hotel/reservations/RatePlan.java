package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** Corporate discount by company code; the guest gets everything back on cancellation. */
    CORPORATE,
    /** Weekend package: fixed price for Friday check-in to Sunday check-out; 50% refund on cancellation. */
    WEEKEND_PACKAGE,
    /** Long stay (7+ nights): 10% discount on daily rate; the guest gets everything back on cancellation. */
    LONG_STAY
}
