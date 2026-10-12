package com.loja.checkout.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OuroStrategy implements NivelClubeStrategy {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    @Override
    public String getCodigo() {
        return "OURO";
    }

    @Override
    public boolean isFreteGratis() {
        return true;
    }

    @Override
    public BigDecimal getPercentualCredito() {
        return PERCENTUAL_CREDITO;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(LIMITE_BRINDE) > 0;
    }
}
