package com.loja.strategy.pagamento;

import com.loja.util.RoundingUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PagamentoPix implements CalculadoraPagamento {
  @Override
  public PagamentoResult calcular(BigDecimal totalPedido, Integer parcelas) {
    BigDecimal desconto = RoundingUtil.round(totalPedido.multiply(new BigDecimal("0.05")));
    BigDecimal totalFinal = RoundingUtil.round(totalPedido.subtract(desconto));
    BigDecimal valorParcela = RoundingUtil.round(totalFinal.divide(new BigDecimal(1), RoundingMode.HALF_EVEN));
    return new PagamentoResult(totalFinal, 1, valorParcela, totalPedido);
  }

  @Override
  public boolean podeAplicar(BigDecimal totalPedido, Integer parcelas) {
    return parcelas == 1;
  }
}
