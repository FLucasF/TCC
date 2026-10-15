package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Os itens do carrinho, com o que se calcula a partir deles. */
public record Pedido(List<Item> itens) {

    public BigDecimal subtotalProdutos() {
        return Dinheiro.arredondar(itens.stream()
                .map(Item::totalDoItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** O peso do pedido nao e arredondado. */
    public BigDecimal pesoKg() {
        return itens.stream()
                .map(Item::pesoDoItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
