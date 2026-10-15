package com.loja.strategy;

import java.math.BigDecimal;
import java.util.List;
import com.loja.dto.ItemCarrinho;

public interface CalculadoraCupom {
  BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens);

  void validar(BigDecimal subtotalProdutos) throws IllegalArgumentException;
}
