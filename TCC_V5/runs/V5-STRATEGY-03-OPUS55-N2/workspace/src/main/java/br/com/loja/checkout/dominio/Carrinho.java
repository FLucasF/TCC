package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Carrinho(List<Item> itens) {

    public Carrinho {
        itens = List.copyOf(itens);
    }

    public BigDecimal subtotalProdutos() {
        return Dinheiro.centavos(itens.stream().map(Item::valorTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso total do pedido, sem arredondamento. */
    public BigDecimal pesoKg() {
        return itens.stream().map(Item::pesoTotalKg).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
