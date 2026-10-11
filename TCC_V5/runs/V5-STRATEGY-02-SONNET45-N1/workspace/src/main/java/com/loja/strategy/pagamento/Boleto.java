package com.loja.strategy.pagamento;

import java.math.BigDecimal;

public class Boleto implements FormaPagamento {
    @Override
    public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
        return new BigDecimal("3.49");
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal total, int parcelas) {
        return total.add(calcularAjuste(total, parcelas));
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean aceita(BigDecimal total) {
        return total.compareTo(new BigDecimal("1000.00")) <= 0;
    }
}
