package com.loja.checkout.modalidade;

import java.math.BigDecimal;

public class RetiradaLojaStrategy implements ModalidadeStrategy {
    @Override
    public boolean estaDisponivel(BigDecimal pesoTotal) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int obterPrazo() {
        return 1;
    }
}
