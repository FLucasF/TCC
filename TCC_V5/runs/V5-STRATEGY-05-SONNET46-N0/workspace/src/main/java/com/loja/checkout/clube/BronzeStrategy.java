package com.loja.checkout.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BronzeStrategy implements NivelClubeStrategy {

    @Override
    public String getCodigo() {
        return "BRONZE";
    }

    @Override
    public boolean isFreteGratis() {
        return false;
    }

    @Override
    public BigDecimal getPercentualCredito() {
        return BigDecimal.ZERO;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
