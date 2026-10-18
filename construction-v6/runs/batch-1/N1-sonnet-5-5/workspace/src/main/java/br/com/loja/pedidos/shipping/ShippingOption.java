package br.com.loja.pedidos.shipping;

import br.com.loja.pedidos.common.Coded;

/** Uma forma de entrega. Cada opção nova entra como uma nova implementação. */
public interface ShippingOption extends Coded {

    /** Calcula frete e prazo; lança SHIPPING_METHOD_UNAVAILABLE se a opção não atende o pedido. */
    ShippingQuote quote(ShippingRequest request);

    /** Falso quando a opção nunca cobra frete (ex.: retirada na loja). */
    default boolean chargesShipping() {
        return true;
    }

    /** Falso quando o frete grátis do clube não vale para a opção (ex.: entrega terceirizada). */
    default boolean clubFreeShippingApplies() {
        return true;
    }
}
