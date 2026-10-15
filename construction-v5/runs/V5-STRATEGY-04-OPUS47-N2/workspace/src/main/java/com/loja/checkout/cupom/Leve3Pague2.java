package com.loja.checkout.cupom;

import com.loja.checkout.calculo.Dinheiro;
import com.loja.checkout.calculo.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public class Leve3Pague2 implements Cupom {
    @Override
    public boolean aplicavel(BigDecimal subtotal, BigDecimal frete, List<ItemCarrinho> itens) {
        return true;
    }

    @Override
    public BigDecimal desconto(BigDecimal subtotal, BigDecimal frete, List<ItemCarrinho> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            int gratis = item.quantidade() / 3;
            total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
        }
        return Dinheiro.cents(total);
    }
}
