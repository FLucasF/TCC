package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    boolean aceitaParcelas(int parcelas);
    boolean isDisponivel(BigDecimal total);
    ResultadoPagamento calcular(BigDecimal total, int parcelas);
}
