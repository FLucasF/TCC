package com.loja.checkout.pagamento;

import java.math.BigDecimal;
import com.loja.checkout.util.Arredonda;

public class PagamentoBoleto implements CalculadoraPagamento {
    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_MAXIMO = new BigDecimal("1000.00");

    @Override
    public PagamentoInfo calcular(BigDecimal total, int parcelas) {
        BigDecimal tarifa = Arredonda.arredonda(TARIFA);
        BigDecimal totalComTarifa = total.add(tarifa);

        return new PagamentoInfo(tarifa, 1, totalComTarifa);
    }

    @Override
    public boolean podeParcelar(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean podeUsar(BigDecimal total) {
        return total.compareTo(LIMITE_MAXIMO) <= 0;
    }
}
