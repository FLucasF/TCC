package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.CalculadoraPagamento;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PagamentoPix implements CalculadoraPagamento {

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean aceitaTotal(BigDecimal total) {
        return true;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
        BigDecimal desconto = total.multiply(new BigDecimal("0.05"))
                .setScale(2, RoundingMode.HALF_EVEN);
        return desconto.negate();
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
