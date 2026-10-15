package com.loja.strategy.pagamento;

import com.loja.util.RoundingUtil;
import java.math.BigDecimal;

public class PagamentoBoleto implements CalculadoraPagamento {
  @Override
  public PagamentoResult calcular(BigDecimal totalPedido, Integer parcelas) {
    BigDecimal tarifa = new BigDecimal("3.49");
    BigDecimal totalFinal = RoundingUtil.round(totalPedido.add(tarifa));
    BigDecimal valorParcela = RoundingUtil.round(totalFinal.divide(new BigDecimal(1), java.math.RoundingMode.HALF_EVEN));
    return new PagamentoResult(totalFinal, 1, valorParcela, totalPedido);
  }

  @Override
  public boolean podeAplicar(BigDecimal totalPedido, Integer parcelas) {
    return parcelas == 1 && totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
  }
}
