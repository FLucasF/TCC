package com.loja.checkout.strategy.pagamento;

public interface EstrategiaPagamento {
  double calcularAjuste(double total, int parcelas);
  boolean parcelasValidas(int parcelas);
  boolean disponivel(double total);
}
