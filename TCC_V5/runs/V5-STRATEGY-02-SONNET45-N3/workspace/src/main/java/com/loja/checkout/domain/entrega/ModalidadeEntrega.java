package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {
    BigDecimal calcularFrete(BigDecimal pesoTotal);
    int obterPrazo();
    boolean estaDisponivel(BigDecimal pesoTotal);
}
