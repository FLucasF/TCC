package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    String getCodigo();
    void verificarParcelamento(int parcelas);
    void verificarDisponibilidade(BigDecimal totalPedido, int parcelas);
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
