package com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    String codigo();

    BigDecimal calcularFrete(BigDecimal pesoTotal);

    int prazoDias();

    default boolean disponivel(BigDecimal pesoTotal) {
        return true;
    }
}
