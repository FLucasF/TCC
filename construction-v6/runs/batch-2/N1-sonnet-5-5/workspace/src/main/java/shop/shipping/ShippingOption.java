package shop.shipping;

/** A way of delivering an order. Each option knows how it charges, its lead time and its limits. */
public interface ShippingOption {
    String code();

    /** Throws ApiException when the option cannot serve the order (unavailable or carrier down). */
    ShippingQuote quote(ShippingContext context);

    /** Whether the order has shipping at all (pickup in store does not). */
    default boolean hasShipping() {
        return true;
    }

    /** Whether club benefits that waive shipping apply (not for third-party couriers). */
    default boolean acceptsClubFreeShipping() {
        return true;
    }
}
