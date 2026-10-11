package com.loja.checkout.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FormaPagamentoPix implements FormaPagamento {
    @Override public String codigo() { return "PIX"; }
    @Override public boolean aceitaParcelas(int parcelas) { return parcelas == 1; }
    @Override public boolean aceita(BigDecimal totalPedido, int parcelas) { return true; }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = totalPedido.multiply(BigDecimal.valueOf(0.05)).setScale(2, RoundingMode.HALF_EVEN);
        return desconto.negate();
    }

    @Override
    public BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas) {
        return totalPedido.add(calcularAjuste(totalPedido, parcelas));
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        return calcularTotalFinal(totalPedido, parcelas);
    }
}
