package com.loja.resumo.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class EntregaEconomica implements OpcaoEntrega {

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg));
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
