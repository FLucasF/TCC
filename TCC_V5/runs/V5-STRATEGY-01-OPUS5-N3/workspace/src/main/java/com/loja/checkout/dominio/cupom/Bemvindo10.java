package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

public final class Bemvindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return Dinheiro.percentual(pedido.subtotalProdutos(), PERCENTUAL);
    }
}
