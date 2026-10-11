package com.loja.strategy;

import java.math.BigDecimal;

public interface CalculadoraSeguro {
  BigDecimal calcularSeguro(BigDecimal subtotalProdutos);
}
