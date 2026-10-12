package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class EntregaExpressa implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public boolean aceita(BigDecimal pesoKgTotal) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKgTotal) {
        return new BigDecimal("25.00")
                .add(new BigDecimal("4.50").multiply(pesoKgTotal))
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int prazoEntregaDias() {
        return 2;
    }
}
