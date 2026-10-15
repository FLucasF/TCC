package com.loja.checkout.domain.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Ganha 5% em crédito, não paga frete nunca e ganha brinde acima de R$ 500. */
@Component
public class Ouro implements NivelClube {

    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal percentualCredito() {
        return new BigDecimal("0.05");
    }

    @Override
    public boolean freteGratis() {
        return true;
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(LIMITE_BRINDE) > 0;
    }
}
