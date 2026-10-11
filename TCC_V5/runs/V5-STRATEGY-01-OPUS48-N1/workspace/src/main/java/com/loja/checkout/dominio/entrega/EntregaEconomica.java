package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class EntregaEconomica implements PoliticaEntrega {

    private static final BigDecimal FIXO = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal custo(BigDecimal pesoKg) {
        return FIXO.add(POR_KG.multiply(pesoKg));
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
