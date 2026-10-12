package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;

import java.math.BigDecimal;
import java.util.List;

public class Bemvindo10 implements Cupom {
    @Override
    public boolean aplicavel(List<Item> itens, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal desconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal freteExibido) {
        return Dinheiro.arredonda(subtotalProdutos.multiply(new BigDecimal("0.10")));
    }
}
