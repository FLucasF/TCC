package com.loja.checkout.strategy.pagamento;

import com.loja.checkout.service.RoundingService;

public class CartaoPagamento implements EstrategiaPagamento {
  private RoundingService rounding;

  public CartaoPagamento(RoundingService rounding) {
    this.rounding = rounding;
  }

  @Override
  public double calcularAjuste(double total, int parcelas) {
    if (parcelas <= 3) {
      return 0.0;
    }
    double taxaMensal = 0.0199;
    double parcela = total * taxaMensal / (1 - Math.pow(1 + taxaMensal, -parcelas));
    double totalComJuros = parcela * parcelas;
    totalComJuros = rounding.round(totalComJuros);
    return totalComJuros - total;
  }

  @Override
  public boolean parcelasValidas(int parcelas) {
    return parcelas >= 1 && parcelas <= 12;
  }

  @Override
  public boolean disponivel(double total) {
    return true;
  }

}
