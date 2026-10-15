package br.com.loja.checkout.entrega;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class EntregaExpressa implements Entrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("25.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("4.50");

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public int prazoDias() {
        return 2;
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return TAXA_FIXA.add(TAXA_POR_KG.multiply(pesoKg));
    }
}
