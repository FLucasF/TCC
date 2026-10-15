package com.loja.checkout.dominio;

import java.math.BigDecimal;

public interface CalculadoraPagamento {
    boolean aceitaParcelas(int parcelas);
    boolean aceitaTotal(BigDecimal total);
    BigDecimal calcularAjuste(BigDecimal total, int parcelas);
    BigDecimal calcularTotalFinal(BigDecimal total, int parcelas);
    BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas);
}
