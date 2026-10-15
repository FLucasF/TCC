package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ItemPedido;

import java.math.BigDecimal;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graca. */
public final class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.centavos(contexto.pedido().itens().stream()
                .map(this::gratuitasDoItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal gratuitasDoItem(ItemPedido item) {
        int gratuitas = item.quantidade() / 3;
        return item.precoUnitario().multiply(BigDecimal.valueOf(gratuitas));
    }
}
