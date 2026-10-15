package com.loja.checkout.strategy;

import java.math.BigDecimal;

public interface FormaPagamento {
    BigDecimal calcularAjuste(BigDecimal total, Integer parcelas);
    BigDecimal calcularValorParcela(BigDecimal total, Integer parcelas);
    boolean parcelaPermitida(Integer parcelas);
    boolean podeAtender(BigDecimal total);
    String nome();
}
