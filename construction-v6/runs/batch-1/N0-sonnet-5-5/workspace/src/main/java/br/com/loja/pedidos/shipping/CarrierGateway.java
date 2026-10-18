package br.com.loja.pedidos.shipping;

import java.math.BigDecimal;

/** A carrier that quotes price and deadline. */
public interface CarrierGateway {

    CarrierResult quote(String zipCode, long weightGrams, ServiceLevel level);

    sealed interface CarrierResult permits Quoted, NotServed, Unavailable {
    }

    record Quoted(BigDecimal price, long etaHours) implements CarrierResult {
    }

    record NotServed() implements CarrierResult {
    }

    record Unavailable() implements CarrierResult {
    }
}
