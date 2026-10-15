package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Itens já validados do pedido, com o subtotal e o peso calculados. */
public record Carrinho(List<Item> itens) {

    public Carrinho {
        itens = List.copyOf(itens);
    }

    /** Soma dos produtos (preço × quantidade), em centavos. */
    public BigDecimal subtotal() {
        return Dinheiro.arredondar(itens.stream()
                .map(Item::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso total do pedido em kg, sem arredondar. */
    public BigDecimal pesoKg() {
        return itens.stream()
                .map(Item::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
