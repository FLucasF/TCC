package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

/** Os produtos do pedido, ja validados. */
public record Carrinho(List<Item> itens) {

    public BigDecimal subtotalProdutos() {
        return Dinheiro.centavos(soma(Item::total));
    }

    /** Peso do pedido, sem arredondar. */
    public BigDecimal pesoKg() {
        return soma(Item::peso);
    }

    private BigDecimal soma(Function<Item, BigDecimal> parte) {
        return itens.stream().map(parte).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
