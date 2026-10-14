package com.loja.orders.shipping;

/**
 * Uma forma de entrega. Para criar uma nova, basta um novo {@code @Component} que implemente esta
 * interface: cada uma tem seu jeito de cobrar, seu prazo e suas limitações.
 */
public interface ShippingOption {

    /** O código usado pelo site, como {@code EXPRESS}. */
    String code();

    /**
     * O frete e o prazo para este pedido. Recusa com {@code SHIPPING_METHOD_UNAVAILABLE} quando a
     * forma de entrega não atende o pedido, ou {@code CARRIER_UNAVAILABLE} quando quem calcula está
     * fora do ar.
     */
    ShippingQuote quote(ShippingRequest request);

    /** Se esta entrega cobra frete (a retirada na loja não cobra, então não cabe cupom de frete). */
    default boolean hasShippingFee() {
        return true;
    }

    /** Se o frete grátis do clube vale nesta entrega (o motoboy é terceirizado e não entra). */
    default boolean coveredByClubFreeShipping() {
        return true;
    }
}
