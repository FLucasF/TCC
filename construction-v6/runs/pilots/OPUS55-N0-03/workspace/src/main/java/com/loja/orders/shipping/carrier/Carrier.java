package com.loja.orders.shipping.carrier;

/** A transportadora que calcula frete e prazo. Trocar de transportadora é trocar esta implementação. */
public interface Carrier {

    /**
     * @throws ZipNotServedException quando a transportadora não atende o CEP
     * @throws CarrierUnavailableException quando a transportadora está fora do ar
     */
    CarrierQuote quote(String zipCode, int weightGrams, CarrierService service);
}
