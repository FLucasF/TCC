package com.loja.checkout.domain.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class Expressa implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("25.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("4.50");

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotal) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return TAXA_FIXA.add(TAXA_POR_KG.multiply(pesoTotal))
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int prazoDias() {
        return 2;
    }
}
