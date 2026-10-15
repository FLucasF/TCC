package com.loja.strategy.pagamento;

import com.loja.util.Arredondamento;
import java.math.BigDecimal;

public class Pix implements FormaPagamento {
    @Override
    public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
        BigDecimal desconto = Arredondamento.arredondar(
            total.multiply(new BigDecimal("0.05"))
        );
        return desconto.negate();
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
        return true;
    }
}
