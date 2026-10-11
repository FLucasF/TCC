package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    int prazoDias();

    boolean disponivel(BigDecimal pesoTotalKg);
}
