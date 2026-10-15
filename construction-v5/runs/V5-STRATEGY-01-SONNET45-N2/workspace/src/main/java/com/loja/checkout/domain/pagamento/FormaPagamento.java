package com.loja.checkout.domain.pagamento;

public interface FormaPagamento {
    boolean aceitaParcelas(int parcelas);
    boolean disponivel(double totalPedido);
    ResultadoPagamento calcular(double totalPedido, int parcelas);
}
