package com.loja.dominio.nivel;

import java.math.BigDecimal;

public class Ouro implements NivelClube {
    private static final BigDecimal TAXA = new BigDecimal("0.05");
    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    @Override public BigDecimal taxaCredito() { return TAXA; }
    @Override public boolean freteGratis() { return true; }
    @Override public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(LIMITE_BRINDE) > 0;
    }
}
