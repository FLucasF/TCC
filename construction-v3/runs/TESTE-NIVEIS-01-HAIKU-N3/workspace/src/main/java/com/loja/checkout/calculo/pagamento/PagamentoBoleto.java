package com.loja.checkout.calculo.pagamento;

import com.loja.checkout.util.Arredondador;
import java.math.BigDecimal;

public class PagamentoBoleto implements PagamentoCalculador {
    private static final BigDecimal TAXA = BigDecimal.valueOf(3.49);

    @Override
    public BigDecimal calcularAjuste(BigDecimal total) {
        return TAXA;
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
