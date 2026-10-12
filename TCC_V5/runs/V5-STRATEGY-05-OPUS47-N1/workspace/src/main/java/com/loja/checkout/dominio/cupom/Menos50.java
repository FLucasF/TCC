package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;

import java.math.BigDecimal;
import java.util.List;

public class Menos50 implements Cupom {
    private static final BigDecimal MINIMO = new BigDecimal("300");

    @Override
    public boolean aplicavel(List<Item> itens, BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO) >= 0;
    }

    @Override
    public BigDecimal desconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal freteExibido) {
        return Dinheiro.arredonda(new BigDecimal("50.00"));
    }
}
