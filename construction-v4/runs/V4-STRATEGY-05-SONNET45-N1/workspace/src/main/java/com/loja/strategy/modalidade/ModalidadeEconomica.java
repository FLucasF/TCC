package com.loja.strategy.modalidade;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ModalidadeEconomica implements Modalidade {
    private static final BigDecimal BASE = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");
    private static final int PRAZO = 7;

    @Override
    public boolean isDisponivel(BigDecimal pesoTotal) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return BASE.add(POR_KG.multiply(pesoTotal))
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int getPrazoEntregaDias() {
        return PRAZO;
    }
}
