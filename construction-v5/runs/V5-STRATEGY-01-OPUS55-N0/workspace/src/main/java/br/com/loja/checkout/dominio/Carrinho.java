package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Itens do pedido já validados, com os totais usados pelas demais regras. */
public record Carrinho(List<Item> itens) {

    public Carrinho {
        itens = List.copyOf(itens);
    }

    /** Soma dos produtos, arredondada para centavos. */
    public BigDecimal subtotalProdutos() {
        return Dinheiro.centavos(itens.stream().map(Item::valorTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso total do pedido, sem arredondamento. */
    public BigDecimal pesoTotalKg() {
        return itens.stream().map(Item::pesoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
