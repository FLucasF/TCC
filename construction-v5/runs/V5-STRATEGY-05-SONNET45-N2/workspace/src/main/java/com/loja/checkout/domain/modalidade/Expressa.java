package com.loja.checkout.domain.modalidade;

import java.math.BigDecimal;

public class Expressa implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        BigDecimal base = new BigDecimal("25.00");
        BigDecimal porKg = new BigDecimal("4.50");
        return base.add(porKg.multiply(pesoTotal));
    }

    @Override
    public int getPrazoEntregaDias() {
        return 2;
    }

    @Override
    public boolean estaDisponivel(BigDecimal pesoTotal) {
        return true;
    }
}
