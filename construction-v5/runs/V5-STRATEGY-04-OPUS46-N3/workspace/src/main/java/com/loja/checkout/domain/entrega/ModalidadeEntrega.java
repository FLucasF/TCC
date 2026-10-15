package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    String codigo();

    BigDecimal calcularFrete(BigDecimal pesoKg);

    int prazoDias();

    boolean disponivel(BigDecimal pesoKg);
}
