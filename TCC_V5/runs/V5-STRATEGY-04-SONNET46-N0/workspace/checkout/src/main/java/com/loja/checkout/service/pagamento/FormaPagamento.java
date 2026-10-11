package com.loja.checkout.service.pagamento;

public interface FormaPagamento {
    String getCodigo();
    boolean parcelasValidas(int parcelas);
    boolean aceitaTotal(double total);
    ResultadoPagamento calcular(double totalPedido, int parcelas);
}
