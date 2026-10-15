package com.loja.checkout.pagamento;

import java.math.BigDecimal;
import com.loja.checkout.util.Arredonda;

public class PagamentoPix implements CalculadoraPagamento {
    @Override
    public PagamentoInfo calcular(BigDecimal total, int parcelas) {
        BigDecimal desconto = Arredonda.arredonda(total.multiply(new BigDecimal("0.05")));
        BigDecimal ajuste = desconto.negate();
        BigDecimal totalComAjuste = total.add(ajuste);

        return new PagamentoInfo(ajuste, 1, totalComAjuste);
    }

    @Override
    public boolean podeParcelar(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean podeUsar(BigDecimal total) {
        return true;
    }
}
