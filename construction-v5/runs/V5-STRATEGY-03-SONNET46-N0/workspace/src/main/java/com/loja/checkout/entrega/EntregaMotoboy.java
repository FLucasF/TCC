package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class EntregaMotoboy implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("18.00");
    private static final BigDecimal LIMITE_PESO_KG = new BigDecimal("5");

    @Override
    public String getCodigo() {
        return "MOTOBOY";
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return TAXA_FIXA.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int getPrazoEntregaDias() {
        return 0;
    }

    @Override
    public boolean aceita(BigDecimal pesoTotalKg) {
        return pesoTotalKg.compareTo(LIMITE_PESO_KG) <= 0;
    }
}
