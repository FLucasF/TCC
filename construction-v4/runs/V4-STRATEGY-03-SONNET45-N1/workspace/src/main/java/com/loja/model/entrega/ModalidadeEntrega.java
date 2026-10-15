package com.loja.model.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {
    BigDecimal calcularFrete(BigDecimal pesoTotalKg);
    int getPrazoDias();
    boolean aceitaPedido(BigDecimal pesoTotalKg);
}
