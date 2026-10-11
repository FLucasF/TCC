package com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    String codigo();

    boolean disponivel(BigDecimal pesoTotalKg);

    ResultadoEntrega calcular(BigDecimal pesoTotalKg);
}
