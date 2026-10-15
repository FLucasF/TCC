package com.loja.checkout.estrategia.modalidade;

import java.math.BigDecimal;

public interface ModalidadeEntrega {
    BigDecimal calcularFrete(BigDecimal pesoTotal);
    int obterPrazo();
    boolean estaDisponivel(BigDecimal pesoTotal);
}
