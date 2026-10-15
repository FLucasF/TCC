package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;

import java.math.BigDecimal;
import java.util.List;

public class Leve3Pague2 implements Cupom {
    @Override
    public boolean aplicavel(List<Item> itens, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal desconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal freteExibido) {
        BigDecimal total = itens.stream()
                .map(i -> i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade() / 3)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.arredonda(total);
    }
}
