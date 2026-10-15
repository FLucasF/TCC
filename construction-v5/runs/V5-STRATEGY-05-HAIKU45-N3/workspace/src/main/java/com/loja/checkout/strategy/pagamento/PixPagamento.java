package com.loja.checkout.strategy.pagamento;

import com.loja.checkout.service.RoundingService;

public class PixPagamento implements EstrategiaPagamento {
  private RoundingService rounding;

  public PixPagamento(RoundingService rounding) {
    this.rounding = rounding;
  }

  @Override
  public double calcularAjuste(double total, int parcelas) {
    double desconto = rounding.round(total * 0.05);
    return -desconto;
  }

  @Override
  public boolean parcelasValidas(int parcelas) {
    return parcelas == 1;
  }

  @Override
  public boolean disponivel(double total) {
    return true;
  }

}
