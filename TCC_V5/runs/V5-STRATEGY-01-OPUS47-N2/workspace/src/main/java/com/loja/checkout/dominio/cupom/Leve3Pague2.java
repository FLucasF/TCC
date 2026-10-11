package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;
import java.math.BigDecimal;
import java.util.List;

public final class Leve3Pague2 implements Cupom {
    public boolean aplicavel(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
        return true;
    }

    public BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
        BigDecimal total = BigDecimal.ZERO;
        for (Item it : itens) {
            int gratis = it.quantidade() / 3;
            if (gratis > 0) {
                total = total.add(it.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
            }
        }
        return Dinheiro.arredondar(total);
    }
}
