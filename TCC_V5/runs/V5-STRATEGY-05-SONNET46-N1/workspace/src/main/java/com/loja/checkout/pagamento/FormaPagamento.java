package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    String codigo();
    void validarParcelas(int parcelas);
    void validarDisponibilidade(BigDecimal totalPedido);
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
