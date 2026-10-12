package com.loja.checkout.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PrataStrategy implements NivelClubeStrategy {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public String getCodigo() {
        return "PRATA";
    }

    @Override
    public boolean isFreteGratis() {
        return false;
    }

    @Override
    public BigDecimal getPercentualCredito() {
        return PERCENTUAL_CREDITO;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
