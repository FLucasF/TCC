package com.loja.checkout.strategy;

import java.math.BigDecimal;

public interface FormaPagamento {
    String getCodigo();
    boolean parcelamentoValido(int parcelas);
    boolean aceitaTotal(BigDecimal totalPedido);
    BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas);
    BigDecimal calcularValorParcela(BigDecimal totalPedido, BigDecimal ajuste, int parcelas);
}
