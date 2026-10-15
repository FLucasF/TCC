package com.loja.checkout.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 5% dos produtos em crédito, frete grátis sempre e brinde acima de R$ 500,00 em produtos. */
@Component
public class NivelOuro implements NivelClube {

    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

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
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
    }
}
