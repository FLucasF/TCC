package com.loja.strategy;

import java.math.BigDecimal;
import java.util.List;
import com.loja.dto.ItemCarrinho;
import com.loja.util.Arredondador;

public class CupomBemvindo10 implements CalculadoraCupom {
  @Override
  public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
    return Arredondador.arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
  }

  @Override
  public void validar(BigDecimal subtotalProdutos) throws IllegalArgumentException {
  }
}
