package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Expressa implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("25.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("4.50");

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return TAXA_FIXA.add(TAXA_POR_KG.multiply(pesoTotal))
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int prazoEntregaDias() {
        return 2;
    }

    @Override
    public void validarDisponibilidade(BigDecimal pesoTotal) {
        // sem restrição
    }
}
