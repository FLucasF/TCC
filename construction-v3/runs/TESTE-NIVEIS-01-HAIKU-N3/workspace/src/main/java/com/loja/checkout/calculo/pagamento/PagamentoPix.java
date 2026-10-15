package com.loja.checkout.calculo.pagamento;

import com.loja.checkout.util.Arredondador;
import java.math.BigDecimal;

public class PagamentoPix implements PagamentoCalculador {
    @Override
    public BigDecimal calcularAjuste(BigDecimal total) {
        BigDecimal desconto = total.multiply(BigDecimal.valueOf(0.05));
        return Arredondador.arredondar(desconto).negate();
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas) {
        return Arredondador.arredondar(totalFinal.divide(BigDecimal.valueOf(parcelas), 10, java.math.RoundingMode.HALF_EVEN));
    }

    @Override
    public int getParcelasMaximas() {
        return 1;
    }
}
