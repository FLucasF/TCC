package com.loja.roupas.domain.formas;

import com.loja.roupas.domain.FormaPagamento;
import com.loja.roupas.domain.util.Arredondador;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PagamentoPix implements FormaPagamento {
    @Override
    public boolean estaDisponivel(Integer parcelas, BigDecimal total) {
        return parcelas == 1;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, Integer parcelas) {
        BigDecimal desconto = total.multiply(new BigDecimal("0.05"));
        desconto = Arredondador.arredondar(desconto);
        return desconto.negate();
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalFinal, Integer parcelas) {
        return Arredondador.arredondar(totalFinal.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN));
    }
}
