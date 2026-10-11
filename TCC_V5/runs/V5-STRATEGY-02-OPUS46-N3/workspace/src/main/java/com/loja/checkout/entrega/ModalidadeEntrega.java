package com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    String codigo();

    BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    int prazoDias();

    default boolean disponivel(BigDecimal pesoTotalKg) {
        return true;
    }
}
