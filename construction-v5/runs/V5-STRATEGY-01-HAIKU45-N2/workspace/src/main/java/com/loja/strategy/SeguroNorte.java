package com.loja.strategy;

import java.math.BigDecimal;
import com.loja.util.Arredondador;

public class SeguroNorte implements CalculadoraSeguro {
  @Override
  public BigDecimal calcularSeguro(BigDecimal subtotalProdutos) {
    return Arredondador.arredondar(subtotalProdutos.multiply(new BigDecimal("0.025")));
  }
}
