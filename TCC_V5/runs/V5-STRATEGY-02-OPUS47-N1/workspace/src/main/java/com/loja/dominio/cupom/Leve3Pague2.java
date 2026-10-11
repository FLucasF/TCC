package com.loja.dominio.cupom;

import com.loja.dominio.Dinheiro;
import com.loja.dominio.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public class Leve3Pague2 implements Cupom {
    @Override public boolean aplicavel(List<ItemCarrinho> itens, BigDecimal subtotal) { return true; }

    @Override public BigDecimal desconto(List<ItemCarrinho> itens, BigDecimal subtotal, BigDecimal frete) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemCarrinho i : itens) {
            int gratis = i.quantidade() / 3;
            if (gratis > 0) {
                total = total.add(i.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
            }
        }
        return Dinheiro.centavos(total);
    }
}
