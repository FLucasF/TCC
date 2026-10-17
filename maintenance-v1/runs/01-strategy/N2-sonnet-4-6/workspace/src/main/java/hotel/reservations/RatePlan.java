package hotel.reservations;

public enum RatePlan {
    /** Full price; full refund on cancellation. */
    STANDARD,
    /** 15% off; no refund on cancellation. */
    NON_REFUNDABLE,
    /** Company discount (12–25% depending on company code); full refund on cancellation. */
    CORPORATE,
    /** Fixed package price for exactly Friday–Sunday; 50% refund on cancellation. */
    WEEKEND_PACKAGE,
    /** 10% off for stays of 7 nights or more; full refund on cancellation. */
    LONG_STAY
}
