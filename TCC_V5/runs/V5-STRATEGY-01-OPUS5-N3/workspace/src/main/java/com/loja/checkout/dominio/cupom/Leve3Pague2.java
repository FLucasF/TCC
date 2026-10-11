package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/** A cada 3 unidades de um mesmo item, uma sai de graça. */
public final class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return pedido.itens().stream()
                .map(this::descontoDoItem)
                .reduce(Dinheiro.ZERO, BigDecimal::add);
    }

    private BigDecimal descontoDoItem(Item item) {
        int gratis = item.quantidade() / 3;
        return Dinheiro.centavos(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
    }
}
