package com.loja.model.pagamento;

import java.math.BigDecimal;

public class Boleto implements FormaPagamento {
    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal MAXIMO = new BigDecimal("1000.00");

    @Override
    public BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas) {
        return totalPedido.add(TARIFA);
    }

    @Override
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean aceitaPedido(BigDecimal totalPedido) {
        return totalPedido.compareTo(MAXIMO) <= 0;
    }
}
