package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Carrinho já validado, com os valores derivados que as regras de negócio consultam. */
public record Pedido(List<Item> itens) {

    public Pedido {
        itens = List.copyOf(itens);
    }

    /** Soma dos produtos (preço × quantidade), arredondada para centavos. */
    public BigDecimal subtotalProdutos() {
        return Dinheiro.arredondar(itens.stream()
                .map(Item::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso total do pedido em kg, sem arredondamento. */
    public BigDecimal pesoTotalKg() {
        return itens.stream()
                .map(Item::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
