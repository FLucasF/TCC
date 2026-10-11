package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;

import java.math.BigDecimal;
import java.util.List;

public class Leve3Pague2 implements Cupom {
    @Override public String codigo() { return "LEVE3PAGUE2"; }
    @Override public boolean aplicavel(List<Item> itens, BigDecimal subtotal) { return true; }
    @Override public BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
        BigDecimal total = BigDecimal.ZERO;
        for (Item item : itens) {
            int gratis = item.quantidade() / 3;
            total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
        }
        return Dinheiro.arredondar(total);
    }
}
