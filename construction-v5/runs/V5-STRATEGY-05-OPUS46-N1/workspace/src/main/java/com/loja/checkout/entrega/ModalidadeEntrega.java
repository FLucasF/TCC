package com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    String codigo();

    BigDecimal calcularFrete(BigDecimal pesoKg);

    int prazoDias();

    default boolean disponivel(BigDecimal pesoKg) {
        return true;
    }
}
