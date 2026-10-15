package com.loja.checkout.clube;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.entrega.ModalidadeEntrega;

import java.math.BigDecimal;

/** Ganha 2% dos produtos de volta em credito. */
public final class Prata implements NivelClube {

    private static final BigDecimal CREDITO = new BigDecimal("0.02");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, CREDITO);
    }

    @Override
    public BigDecimal frete(ModalidadeEntrega entrega, Pedido pedido) {
        return entrega.frete(pedido);
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
