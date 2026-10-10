package com.loja.service.estrategia;

import com.loja.util.Arredondador;
import java.math.BigDecimal;

public class PagamentoBoleto implements CalculoPagamento {
    private static final BigDecimal TAXA_BANCO = new BigDecimal("3.49");

    @Override
    public BigDecimal calcularParcela(BigDecimal total, Integer parcelas) {
        return Arredondador.arredondarParaCentavos(total.add(TAXA_BANCO));
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal parcelaCalculada, Integer parcelas, BigDecimal totalAntesAjuste) {
        return Arredondador.arredondarParaCentavos(TAXA_BANCO);
    }

    @Override
    public boolean validarParcelas(Integer parcelas) {
        return parcelas == null || parcelas == 1;
    }
}
