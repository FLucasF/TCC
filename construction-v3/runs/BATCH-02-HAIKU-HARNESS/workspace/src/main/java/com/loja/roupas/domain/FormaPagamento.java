package com.loja.roupas.domain;

import java.math.BigDecimal;

public interface FormaPagamento {
    boolean estaDisponivel(Integer parcelas, BigDecimal total);
    BigDecimal calcularAjuste(BigDecimal total, Integer parcelas);
    BigDecimal calcularValorParcela(BigDecimal total, Integer parcelas);
}
