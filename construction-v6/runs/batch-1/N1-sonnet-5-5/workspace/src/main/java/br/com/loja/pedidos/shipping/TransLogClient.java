package br.com.loja.pedidos.shipping;

import java.util.Optional;

public interface TransLogClient {

    /**
     * @return a cotação, ou vazio quando a TransLog não atende o CEP
     * @throws br.com.loja.pedidos.common.DomainException CARRIER_UNAVAILABLE se a TransLog estiver fora do ar
     */
    Optional<TransLogQuote> quote(String zipCode, long weightGrams, String service);

    record TransLogQuote(int priceCents, int etaHours) {
    }
}
