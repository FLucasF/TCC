package com.loja.checkout.service.entrega;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class EntregaExpressa implements EstrategiaEntrega {

    @Override
    public String getCodigo() {
        return "EXPRESSA";
    }

    @Override
    public boolean atendePedido(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        // 25.00 + 4.50 × pesoTotalKg
        BigDecimal base = new BigDecimal("25.00");
        BigDecimal taxa = new BigDecimal("4.50");
        return base.add(taxa.multiply(pesoTotalKg)).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int prazoEmDias() {
        return 2;
    }
}
