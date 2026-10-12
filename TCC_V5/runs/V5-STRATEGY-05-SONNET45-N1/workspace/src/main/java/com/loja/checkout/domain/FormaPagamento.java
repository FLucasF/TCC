package com.loja.checkout.domain;

import java.math.BigDecimal;

public interface FormaPagamento {
    BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas);
    int numeroParcelas(int parcelasDesejadas);
    BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas);
    boolean aceitaPedido(BigDecimal totalPedido);
    boolean parcelamentoValido(int parcelas);
}
