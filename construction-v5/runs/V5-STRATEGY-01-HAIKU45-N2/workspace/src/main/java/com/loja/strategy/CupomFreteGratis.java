package com.loja.strategy;

import java.math.BigDecimal;
import java.util.List;
import com.loja.dto.ItemCarrinho;

public class CupomFreteGratis implements CalculadoraCupom {
  @Override
  public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
    return frete;
  }

  @Override
  public void validar(BigDecimal subtotalProdutos) throws IllegalArgumentException {
  }
}
