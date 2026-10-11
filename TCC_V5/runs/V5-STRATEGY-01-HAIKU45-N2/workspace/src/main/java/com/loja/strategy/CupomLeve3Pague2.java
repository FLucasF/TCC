package com.loja.strategy;

import java.math.BigDecimal;
import java.util.List;
import com.loja.dto.ItemCarrinho;
import com.loja.util.Arredondador;

public class CupomLeve3Pague2 implements CalculadoraCupom {
  @Override
  public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
    BigDecimal desconto = BigDecimal.ZERO;

    for (ItemCarrinho item : itens) {
      if (item.quantidade != null && item.precoUnitario != null && item.quantidade >= 3) {
        int unidadesGratis = item.quantidade / 3;
        BigDecimal descontoItem = new BigDecimal(unidadesGratis)
            .multiply(BigDecimal.valueOf(item.precoUnitario));
        desconto = desconto.add(descontoItem);
      }
    }

    return Arredondador.arredondar(desconto);
  }

  @Override
  public void validar(BigDecimal subtotalProdutos) throws IllegalArgumentException {
  }
}
