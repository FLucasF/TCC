package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

public class RetiradaLoja implements ModalidadeEntrega {
    @Override
    public boolean estaDisponivel(BigDecimal pesoKg) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int obterPrazo() {
        return 1;
    }
}
