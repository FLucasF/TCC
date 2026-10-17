package hotel.reservations;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE,
    /** Company-negotiated discount (TECNOSUL 12%, AGROVALE 18%, FARMAVIDA 25%); full refund on cancellation. */
    CORPORATE,
    /** Fixed price per room type (STANDARD R$520, DELUXE R$820, SUITE R$1200), Fri–Sun only; 50% refund on cancellation. */
    WEEKEND_PACKAGE,
    /** 10% off stays of 7 nights or more; full refund on cancellation. */
    LONG_STAY
}
