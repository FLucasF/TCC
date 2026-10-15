package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** O carrinho do cliente: os itens e o que se calcula direto deles. */
public record Pedido(List<Item> itens) {

    public Pedido {
        itens = List.copyOf(itens);
    }

    /** Soma dos produtos (preco x quantidade de cada item). */
    public BigDecimal subtotalProdutos() {
        return Dinheiro.centavos(itens.stream()
                .map(Item::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso do pedido em kg, sem arredondar. */
    public BigDecimal pesoKg() {
        return itens.stream()
                .map(Item::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
