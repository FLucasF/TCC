package com.loja.dominio.nivel;

import java.math.BigDecimal;

public class Prata implements NivelClube {
    private static final BigDecimal TAXA = new BigDecimal("0.02");

    @Override public BigDecimal taxaCredito() { return TAXA; }
    @Override public boolean freteGratis() { return false; }
    @Override public boolean ganhaBrinde(BigDecimal subtotalProdutos) { return false; }
}
