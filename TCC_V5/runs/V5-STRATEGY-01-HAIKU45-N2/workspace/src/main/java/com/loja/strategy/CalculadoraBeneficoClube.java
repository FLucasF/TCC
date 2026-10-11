package com.loja.strategy;

import java.math.BigDecimal;

public interface CalculadoraBeneficoClube {
  BigDecimal calcularCredito(BigDecimal subtotalProdutos);

  boolean temFreteGratis();

  boolean temBrinde(BigDecimal subtotalProdutos);
}
