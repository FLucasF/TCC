package br.com.loja.shipping;

/**
 * Uma forma de entrega. Para acrescentar uma nova, basta criar um bean que implemente esta interface.
 */
public interface ShippingOption {

    String code();

    /**
     * Calcula frete e prazo. Lança {@code ApiException} com SHIPPING_METHOD_UNAVAILABLE quando a opção não
     * atende o pedido, ou CARRIER_UNAVAILABLE quando a transportadora está fora do ar.
     */
    ShippingQuote quote(ShippingRequest request);

    /** false quando a opção nunca tem frete (ex.: retirada na loja). */
    default boolean chargesFreight() {
        return true;
    }

    /** false quando o frete é de terceiros e vantagens do clube não o abatem (ex.: motoboy). */
    default boolean freightWaivableByClub() {
        return true;
    }
}
