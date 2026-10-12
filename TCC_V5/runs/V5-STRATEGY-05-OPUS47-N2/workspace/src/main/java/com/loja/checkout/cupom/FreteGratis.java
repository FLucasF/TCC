package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Item;

import java.math.BigDecimal;
import java.util.List;

public final class FreteGratis implements Cupom {
    @Override public boolean aplicavel(List<Item> itens, BigDecimal subtotalProdutos) { return true; }

    @Override public BigDecimal desconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        return frete;
    }
}
