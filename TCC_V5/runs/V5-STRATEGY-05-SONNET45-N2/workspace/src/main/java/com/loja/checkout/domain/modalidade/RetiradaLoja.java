package com.loja.checkout.domain.modalidade;

import java.math.BigDecimal;

public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return BigDecimal.ZERO;
    }

    @Override
    public int getPrazoEntregaDias() {
        return 1;
    }

    @Override
    public boolean estaDisponivel(BigDecimal pesoTotal) {
        return true;
    }
}
