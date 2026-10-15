package com.loja.resumo.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class EntregaExpressa implements OpcaoEntrega {

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg));
    }

    @Override
    public int prazoDias() {
        return 2;
    }
}
