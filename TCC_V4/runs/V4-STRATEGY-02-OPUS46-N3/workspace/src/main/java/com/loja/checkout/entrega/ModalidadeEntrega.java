package com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    String codigo();

    BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    int prazoDias();

    boolean disponivel(BigDecimal pesoTotalKg);
}
