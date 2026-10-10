package com.loja.checkout.strategy.pagamento;

import java.math.BigDecimal;

public interface FormaPagamentoStrategy {
    BigDecimal calcularAjuste(BigDecimal total, int parcelas);

    int getParcelasPermitidas();

    void validar(BigDecimal total, int parcelas);
}
