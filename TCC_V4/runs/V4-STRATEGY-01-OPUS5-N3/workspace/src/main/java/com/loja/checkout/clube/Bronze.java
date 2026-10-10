package com.loja.checkout.clube;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.entrega.ModalidadeEntrega;

import java.math.BigDecimal;

/** So o cadastro: nao ganha nada. */
public final class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
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
