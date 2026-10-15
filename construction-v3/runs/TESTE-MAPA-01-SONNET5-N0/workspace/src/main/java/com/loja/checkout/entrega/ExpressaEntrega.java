package com.loja.checkout.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class ExpressaEntrega implements EntregaStrategy {

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
    public boolean disponivelPara(BigDecimal pesoTotalKg, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg, BigDecimal subtotalProdutos) {
        return TAXA_FIXA.add(pesoTotalKg.multiply(TAXA_POR_KG));
    }
}
