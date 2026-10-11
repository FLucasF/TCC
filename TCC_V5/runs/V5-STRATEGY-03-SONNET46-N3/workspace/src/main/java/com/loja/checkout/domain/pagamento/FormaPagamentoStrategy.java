package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public interface FormaPagamentoStrategy {
    String codigo();
    boolean aceitaParcelas(int parcelas);
    boolean disponivelParaTotal(BigDecimal total);
    ResultadoPagamento calcular(BigDecimal total, int parcelas);
}
