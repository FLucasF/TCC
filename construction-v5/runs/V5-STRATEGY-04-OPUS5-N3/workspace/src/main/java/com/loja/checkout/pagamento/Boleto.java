package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/** Boleto: tarifa fixa do banco e só para pedidos de até R$ 1.000,00. */
class Boleto extends AVista {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
    }

    @Override
    protected BigDecimal totalFinalAVista(BigDecimal totalPedido) {
        return Dinheiro.centavos(totalPedido.add(TARIFA));
    }
}
