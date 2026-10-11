package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Carrinho(List<ItemCarrinho> itens) {

    public BigDecimal subtotal() {
        return itens.stream().map(ItemCarrinho::total).reduce(Dinheiro.ZERO, BigDecimal::add);
    }

    public BigDecimal pesoTotal() {
        return itens.stream().map(ItemCarrinho::pesoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
