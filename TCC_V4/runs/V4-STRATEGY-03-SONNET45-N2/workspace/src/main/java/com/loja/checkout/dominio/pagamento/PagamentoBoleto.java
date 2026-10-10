package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.CalculadoraPagamento;
import java.math.BigDecimal;

public class PagamentoBoleto implements CalculadoraPagamento {

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean aceitaTotal(BigDecimal total) {
        return total.compareTo(new BigDecimal("1000.00")) <= 0;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
        return new BigDecimal("3.49");
    }

    @Override
    public BigDecimal calcularTotalFinal(BigDecimal total, int parcelas) {
        return total.add(calcularAjuste(total, parcelas));
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas) {
        return totalFinal;
    }
}
