package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.FormaPagamento;
import java.math.BigDecimal;

public class Boleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_VALOR = new BigDecimal("1000.00");

    @Override
    public BigDecimal calcularValorFinal(BigDecimal totalPedido, int parcelas) {
        return totalPedido.add(TARIFA);
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean aceita(BigDecimal totalPedido) {
        return totalPedido.compareTo(LIMITE_VALOR) <= 0;
    }
}
