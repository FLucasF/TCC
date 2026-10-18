package br.com.loja.pedidos.shipping;

/** A way of delivering an order. Each option knows its own price, deadline and limitations. */
public interface ShippingOption {

    String code();

    /** @throws br.com.loja.pedidos.common.ApiException when the option cannot serve the request */
    ShippingQuote quote(ShippingRequest request);

    /** Whether club perks that waive shipping apply to this option. */
    default boolean clubFreeShippingApplies() {
        return true;
    }
}
