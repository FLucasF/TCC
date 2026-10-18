package br.com.loja.shipping;

import java.math.BigDecimal;

/** Porta para uma transportadora externa. Trocar de transportadora = nova implementação desta interface. */
public interface CarrierGateway {

    /**
     * @throws ZipNotServedException quando a transportadora não atende o CEP
     * @throws CarrierDownException  quando ela não responde direito
     */
    CarrierQuote quote(String zipCode, long weightGrams, String service);

    record CarrierQuote(BigDecimal price, int days) {
    }

    class ZipNotServedException extends RuntimeException {
    }

    class CarrierDownException extends RuntimeException {
        public CarrierDownException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
