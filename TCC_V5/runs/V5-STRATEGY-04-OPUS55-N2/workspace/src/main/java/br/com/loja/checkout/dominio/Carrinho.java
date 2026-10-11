package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Carrinho(List<Item> itens) {

    public Carrinho {
        itens = List.copyOf(itens);
    }

    /** Soma dos produtos, arredondada para centavos. */
    public BigDecimal subtotal() {
        return Dinheiro.centavos(itens.stream().map(Item::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso total do pedido, sem arredondamento. */
    public BigDecimal pesoKg() {
        return itens.stream().map(Item::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
