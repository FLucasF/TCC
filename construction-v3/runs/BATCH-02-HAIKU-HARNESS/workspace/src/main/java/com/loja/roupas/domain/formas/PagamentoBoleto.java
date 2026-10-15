package com.loja.roupas.domain.formas;

import com.loja.roupas.domain.FormaPagamento;
import com.loja.roupas.domain.util.Arredondador;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PagamentoBoleto implements FormaPagamento {
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");

    @Override
    public boolean estaDisponivel(Integer parcelas, BigDecimal total) {
        return parcelas == 1 && total.compareTo(LIMITE_BOLETO) <= 0;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, Integer parcelas) {
        return TARIFA_BOLETO;
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalFinal, Integer parcelas) {
        return Arredondador.arredondar(totalFinal.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN));
    }
}
