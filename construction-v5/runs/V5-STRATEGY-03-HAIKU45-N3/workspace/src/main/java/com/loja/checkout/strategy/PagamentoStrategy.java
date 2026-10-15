package com.loja.checkout.strategy;

import java.math.BigDecimal;

public interface PagamentoStrategy {
    BigDecimal calcularAjuste(BigDecimal total, Integer parcelas);
    BigDecimal calcularValorParcela(BigDecimal valorFinal, Integer parcelas);
    Integer getParcelasMaximas();
    boolean isDisponivel(BigDecimal total);
}
