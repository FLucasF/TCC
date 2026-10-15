package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    String codigo();
    boolean aceitaParcelas(int parcelas);
    boolean disponivel(BigDecimal total);
    ResultadoPagamento calcular(BigDecimal total, int parcelas);
}
