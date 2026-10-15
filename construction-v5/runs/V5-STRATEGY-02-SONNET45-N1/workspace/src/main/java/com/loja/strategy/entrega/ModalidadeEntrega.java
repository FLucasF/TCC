package com.loja.strategy.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {
    BigDecimal calcularFrete(BigDecimal pesoTotal);
    int getPrazo();
    boolean aceita(BigDecimal pesoTotal);
}
