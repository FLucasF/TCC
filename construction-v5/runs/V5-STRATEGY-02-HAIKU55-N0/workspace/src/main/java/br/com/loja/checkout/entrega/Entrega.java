package br.com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface Entrega {

    String codigo();

    int prazoDias();

    BigDecimal frete(BigDecimal pesoKg);

    default boolean atende(BigDecimal pesoKg) {
        return true;
    }
}
