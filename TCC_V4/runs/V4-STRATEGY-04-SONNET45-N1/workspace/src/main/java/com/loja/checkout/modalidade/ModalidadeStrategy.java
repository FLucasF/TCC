package com.loja.checkout.modalidade;

import java.math.BigDecimal;

public interface ModalidadeStrategy {
    boolean estaDisponivel(BigDecimal pesoTotal);
    BigDecimal calcularFrete(BigDecimal pesoTotal);
    int obterPrazo();
}
