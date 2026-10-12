package com.loja.strategy.pagamento;

import com.loja.util.RoundingUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PagamentoCartao implements CalculadoraPagamento {
  @Override
  public PagamentoResult calcular(BigDecimal totalPedido, Integer parcelas) {
    if (parcelas <= 3) {
      BigDecimal valorParcela = RoundingUtil.round(totalPedido.divide(new BigDecimal(parcelas), RoundingMode.HALF_EVEN));
      BigDecimal totalFinal = RoundingUtil.round(valorParcela.multiply(new BigDecimal(parcelas)));
      return new PagamentoResult(totalFinal, parcelas, valorParcela, totalPedido);
    } else {
      BigDecimal taxa = new BigDecimal("0.0199");
      BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
      BigDecimal potencia = umMaisTaxa.pow(parcelas);
      BigDecimal numerador = totalPedido.multiply(taxa).multiply(potencia);
      BigDecimal denominador = potencia.subtract(BigDecimal.ONE);
      BigDecimal valorParcela = RoundingUtil.round(numerador.divide(denominador, RoundingMode.HALF_EVEN));
      BigDecimal totalFinal = RoundingUtil.round(valorParcela.multiply(new BigDecimal(parcelas)));
      return new PagamentoResult(totalFinal, parcelas, valorParcela, totalPedido);
    }
  }

  @Override
  public boolean podeAplicar(BigDecimal totalPedido, Integer parcelas) {
    return parcelas >= 1 && parcelas <= 12;
  }
}
