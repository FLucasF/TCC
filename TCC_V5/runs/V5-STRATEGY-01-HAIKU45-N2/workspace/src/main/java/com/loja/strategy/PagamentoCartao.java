package com.loja.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.loja.util.Arredondador;

public class PagamentoCartao implements CalculadoraPagamento {
  @Override
  public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
    if (parcelas <= 3) {
      return BigDecimal.ZERO;
    }

    BigDecimal taxa = new BigDecimal("0.0199");
    BigDecimal parcela = calcularValorParcela(total, parcelas);

    BigDecimal totalComJuros = parcela.multiply(new BigDecimal(parcelas));
    BigDecimal jurosTotal = totalComJuros.subtract(total);

    return Arredondador.arredondar(jurosTotal);
  }

  @Override
  public void validar(int parcelas, BigDecimal total) throws IllegalArgumentException {
    if (parcelas < 1 || parcelas > 12) {
      throw new IllegalArgumentException("PARCELAMENTO_INVALIDO");
    }
  }

  @Override
  public BigDecimal calcularValorParcela(BigDecimal totalComAjuste, int parcelas) {
    if (parcelas <= 3) {
      return Arredondador.arredondar(totalComAjuste.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN));
    }

    BigDecimal taxa = new BigDecimal("0.0199");
    BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
    BigDecimal potencia = umMaisTaxa.pow(parcelas);

    BigDecimal numerador = taxa.multiply(potencia);
    BigDecimal denominador = potencia.subtract(BigDecimal.ONE);

    return Arredondador.arredondar(totalComAjuste.multiply(numerador).divide(denominador, 10, RoundingMode.HALF_EVEN));
  }
}
