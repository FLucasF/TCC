package com.loja.strategy;

import java.math.BigDecimal;
import java.util.List;
import com.loja.dto.ItemCarrinho;

public class CupomMenos50 implements CalculadoraCupom {
  @Override
  public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
    return new BigDecimal("50.00");
  }

  @Override
  public void validar(BigDecimal subtotalProdutos) throws IllegalArgumentException {
    if (subtotalProdutos.compareTo(new BigDecimal("300.00")) < 0) {
      throw new IllegalArgumentException("CUPOM_NAO_APLICAVEL");
    }
  }
}
