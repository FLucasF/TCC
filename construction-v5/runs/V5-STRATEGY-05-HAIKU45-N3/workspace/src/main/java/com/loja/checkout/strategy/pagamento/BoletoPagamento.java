package com.loja.checkout.strategy.pagamento;

public class BoletoPagamento implements EstrategiaPagamento {
  @Override
  public double calcularAjuste(double total, int parcelas) {
    return 3.49;
  }

  @Override
  public boolean parcelasValidas(int parcelas) {
    return parcelas == 1;
  }

  @Override
  public boolean disponivel(double total) {
    return total <= 1000.0;
  }

}
