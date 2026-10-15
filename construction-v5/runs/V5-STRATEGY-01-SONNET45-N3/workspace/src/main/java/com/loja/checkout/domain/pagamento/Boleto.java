package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public class Boleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE = new BigDecimal("1000.00");

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        return TARIFA;
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, BigDecimal ajuste, int parcelas) {
        return totalPedido.add(ajuste);
    }

    @Override
    public boolean aceitaParcelamento(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean aceitaPedido(BigDecimal totalPedido) {
        return totalPedido.compareTo(LIMITE) <= 0;
    }
}
