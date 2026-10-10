package com.loja.service.estrategia;

import com.loja.util.Arredondador;
import java.math.BigDecimal;

public class PagamentoPix implements CalculoPagamento {
    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

    @Override
    public BigDecimal calcularParcela(BigDecimal total, Integer parcelas) {
        return Arredondador.arredondarParaCentavos(total);
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal parcelaCalculada, Integer parcelas, BigDecimal totalAntesAjuste) {
        BigDecimal desconto = totalAntesAjuste.multiply(PERCENTUAL_DESCONTO);
        desconto = Arredondador.arredondarParaCentavos(desconto);
        return desconto.negate();
    }

    @Override
    public boolean validarParcelas(Integer parcelas) {
        return parcelas == null || parcelas == 1;
    }
}
