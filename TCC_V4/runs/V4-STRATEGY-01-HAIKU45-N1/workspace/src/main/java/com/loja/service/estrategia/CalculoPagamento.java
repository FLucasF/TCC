package com.loja.service.estrategia;

import java.math.BigDecimal;

public interface CalculoPagamento {
    BigDecimal calcularParcela(BigDecimal total, Integer parcelas);
    BigDecimal calcularAjuste(BigDecimal parcelaCalculada, Integer parcelas, BigDecimal totalAntesAjuste);
    boolean validarParcelas(Integer parcelas);
}
