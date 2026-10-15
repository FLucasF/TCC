package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    String codigo();

    boolean disponivel(BigDecimal pesoTotal);

    BigDecimal calcularFrete(BigDecimal pesoTotal);

    int prazoDias();
}
