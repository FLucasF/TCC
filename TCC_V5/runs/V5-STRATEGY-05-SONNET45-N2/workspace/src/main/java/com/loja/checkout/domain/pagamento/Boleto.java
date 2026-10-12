package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public class Boleto implements FormaPagamento {

    private static final BigDecimal LIMITE = new BigDecimal("1000.00");
    private static final BigDecimal TARIFA = new BigDecimal("3.49");

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return totalPedido.compareTo(LIMITE) <= 0;
    }

    @Override
    public boolean aceitaParcelas(int numeroParcelas) {
        return numeroParcelas == 1;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        return TARIFA;
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        return totalPedido.add(TARIFA);
    }
}
