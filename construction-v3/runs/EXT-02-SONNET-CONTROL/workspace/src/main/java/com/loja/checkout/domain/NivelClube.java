package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.Optional;

public enum NivelClube {

    BRONZE(BigDecimal.ZERO, false, Optional.empty()),
    PRATA(new BigDecimal("0.02"), false, Optional.empty()),
    OURO(new BigDecimal("0.05"), true, Optional.of(new BigDecimal("500.00")));

    private final BigDecimal percentualCredito;
    private final boolean isentoFrete;
    private final Optional<BigDecimal> limiteBrindeProdutos;

    NivelClube(BigDecimal percentualCredito, boolean isentoFrete, Optional<BigDecimal> limiteBrindeProdutos) {
        this.percentualCredito = percentualCredito;
        this.isentoFrete = isentoFrete;
        this.limiteBrindeProdutos = limiteBrindeProdutos;
    }

    public BigDecimal percentualCredito() {
        return percentualCredito;
    }

    public boolean isentoFrete() {
        return isentoFrete;
    }

    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return limiteBrindeProdutos.isPresent() && subtotalProdutos.compareTo(limiteBrindeProdutos.get()) > 0;
    }
}
