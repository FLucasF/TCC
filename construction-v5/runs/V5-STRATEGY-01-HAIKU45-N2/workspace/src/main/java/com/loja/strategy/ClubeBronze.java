package com.loja.strategy;

import java.math.BigDecimal;

public class ClubeBronze implements CalculadoraBeneficoClube {
  @Override
  public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
    return BigDecimal.ZERO;
  }

  @Override
  public boolean temFreteGratis() {
    return false;
  }

  @Override
  public boolean temBrinde(BigDecimal subtotalProdutos) {
    return false;
  }
}
