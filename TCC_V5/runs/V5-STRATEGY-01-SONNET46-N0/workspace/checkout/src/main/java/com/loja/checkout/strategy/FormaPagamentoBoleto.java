package com.loja.checkout.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FormaPagamentoBoleto implements FormaPagamento {
    private static final BigDecimal TARIFA = BigDecimal.valueOf(3.49);
    private static final BigDecimal LIMITE = BigDecimal.valueOf(1000.00);

    @Override public String codigo() { return "BOLETO"; }
    @Override public boolean aceitaParcelas(int parcelas) { return parcelas == 1; }

    @Override
    public boolean aceita(BigDecimal totalPedido, int parcelas) {
        return totalPedido.compareTo(LIMITE) <= 0;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        return TARIFA;
    }

    @Override
    public BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas) {
        return totalPedido.add(TARIFA).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        return calcularTotalFinal(totalPedido, parcelas);
    }
}
