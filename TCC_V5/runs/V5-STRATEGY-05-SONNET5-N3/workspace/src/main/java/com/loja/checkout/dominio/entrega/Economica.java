package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Economica implements OpcaoEntrega {

    private static final BigDecimal BASE = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal custo(BigDecimal pesoTotalKg) {
        return BASE.add(POR_KG.multiply(pesoTotalKg));
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
