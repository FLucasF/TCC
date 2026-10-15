package com.loja.checkout.domain;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(BigDecimal.ZERO, false, BigDecimal.valueOf(0)),
    PRATA(BigDecimal.valueOf(0.02), false, BigDecimal.valueOf(0)),
    OURO(BigDecimal.valueOf(0.05), true, BigDecimal.valueOf(500));

    private final BigDecimal percentualCredito;
    private final boolean gratisFrete;
    private final BigDecimal limiteParaBrinde;

    NivelClube(BigDecimal percentualCredito, boolean gratisFrete, BigDecimal limiteParaBrinde) {
        this.percentualCredito = percentualCredito;
        this.gratisFrete = gratisFrete;
        this.limiteParaBrinde = limiteParaBrinde;
    }

    public BigDecimal calcularCredito(BigDecimal subtotal) {
        return subtotal.multiply(percentualCredito);
    }

    public boolean isGratisFrete() {
        return gratisFrete;
    }

    public boolean temBrinde(BigDecimal subtotal) {
        return this == OURO && subtotal.compareTo(limiteParaBrinde) > 0;
    }
}
