package com.loja.checkout.strategy;

import java.math.BigDecimal;

public interface FormaPagamento {
    String codigo();
    boolean aceitaParcelas(int parcelas);
    boolean aceita(BigDecimal totalPedido, int parcelas);
    BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas);
    BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas);
    BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas);
}
