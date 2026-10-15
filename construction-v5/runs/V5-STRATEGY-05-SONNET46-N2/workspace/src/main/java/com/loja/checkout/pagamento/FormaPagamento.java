package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    String codigo();
    boolean aceitaParcelas(int parcelas);
    boolean aceita(BigDecimal totalPedido);
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
