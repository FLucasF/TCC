package com.loja.strategy;

import java.math.BigDecimal;
import com.loja.util.Arredondador;

public class ClubeOuro implements CalculadoraBeneficoClube {
  @Override
  public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
    return Arredondador.arredondar(subtotalProdutos.multiply(new BigDecimal("0.05")));
  }

  @Override
  public boolean temFreteGratis() {
    return true;
  }

  @Override
  public boolean temBrinde(BigDecimal subtotalProdutos) {
    return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
  }
}
