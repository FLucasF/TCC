package com.loja.dominio.nivel;

import java.math.BigDecimal;

public class Bronze implements NivelClube {
    @Override public BigDecimal taxaCredito() { return BigDecimal.ZERO; }
    @Override public boolean freteGratis() { return false; }
    @Override public boolean ganhaBrinde(BigDecimal subtotalProdutos) { return false; }
}
