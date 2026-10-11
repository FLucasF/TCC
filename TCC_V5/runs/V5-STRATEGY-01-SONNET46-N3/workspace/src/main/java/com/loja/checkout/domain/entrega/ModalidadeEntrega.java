package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {
    String codigo();
    boolean disponivel(BigDecimal pesoKg);
    BigDecimal calcularFrete(BigDecimal pesoKg);
    int prazoDias();
}
