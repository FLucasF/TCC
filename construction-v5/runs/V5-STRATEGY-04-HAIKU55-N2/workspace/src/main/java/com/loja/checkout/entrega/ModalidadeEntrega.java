package com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    String codigo();

    int prazoDias();

    BigDecimal custo(BigDecimal pesoKg);

    default boolean atende(BigDecimal pesoKg) {
        return true;
    }
}
