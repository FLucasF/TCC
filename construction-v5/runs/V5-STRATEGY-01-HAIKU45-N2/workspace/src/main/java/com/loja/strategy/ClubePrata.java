package com.loja.strategy;

import java.math.BigDecimal;
import com.loja.util.Arredondador;

public class ClubePrata implements CalculadoraBeneficoClube {
  @Override
  public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
    return Arredondador.arredondar(subtotalProdutos.multiply(new BigDecimal("0.02")));
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
