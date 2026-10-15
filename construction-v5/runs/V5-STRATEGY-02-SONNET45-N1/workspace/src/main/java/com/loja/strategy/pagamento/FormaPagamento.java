package com.loja.strategy.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    BigDecimal calcularAjuste(BigDecimal total, int parcelas);
    BigDecimal calcularValorParcela(BigDecimal total, int parcelas);
    boolean aceitaParcelas(int parcelas);
    boolean aceita(BigDecimal total);
}
