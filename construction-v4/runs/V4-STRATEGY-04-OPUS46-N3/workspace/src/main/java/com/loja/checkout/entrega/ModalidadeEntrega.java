package com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    BigDecimal calcularFrete(BigDecimal pesoKg);

    int prazoDias();

    boolean disponivel(BigDecimal pesoKg);
}
