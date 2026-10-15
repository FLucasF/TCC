package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Item;

import java.math.BigDecimal;
import java.util.List;

public final class Leve3Pague2 implements Cupom {
    @Override public boolean aplicavel(List<Item> itens, BigDecimal subtotalProdutos) { return true; }

    @Override public BigDecimal desconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        BigDecimal total = BigDecimal.ZERO;
        for (Item item : itens) {
            int gratis = item.quantidade() / 3;
            total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
        }
        return total;
    }
}
