package com.loja.checkout.service.entrega;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class EntregaEconomica implements EstrategiaEntrega {

    @Override
    public String getCodigo() {
        return "ECONOMICA";
    }

    @Override
    public boolean atendePedido(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        // 12.00 + 2.00 × pesoTotalKg
        BigDecimal base = new BigDecimal("12.00");
        BigDecimal taxa = new BigDecimal("2.00");
        return base.add(taxa.multiply(pesoTotalKg)).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int prazoEmDias() {
        return 7;
    }
}
