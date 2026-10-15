package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/** Pix: 5% de desconto no total do pedido. */
class Pix extends AVista {

    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override
    public String codigo() {
        return "PIX";
    }

    @Override
    protected BigDecimal totalFinalAVista(BigDecimal totalPedido) {
        return Dinheiro.centavos(totalPedido.subtract(Dinheiro.percentual(totalPedido, DESCONTO)));
    }
}
