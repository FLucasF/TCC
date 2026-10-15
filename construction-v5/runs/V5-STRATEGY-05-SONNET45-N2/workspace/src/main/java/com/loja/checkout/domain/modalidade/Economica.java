package com.loja.checkout.domain.modalidade;

import java.math.BigDecimal;

public class Economica implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        BigDecimal base = new BigDecimal("12.00");
        BigDecimal porKg = new BigDecimal("2.00");
        return base.add(porKg.multiply(pesoTotal));
    }

    @Override
    public int getPrazoEntregaDias() {
        return 7;
    }

    @Override
    public boolean estaDisponivel(BigDecimal pesoTotal) {
        return true;
    }
}
