package br.com.loja.checkout.pedido;

import java.math.BigDecimal;
import java.util.List;

public record Carrinho(List<ItemPedido> itens) {

    public Carrinho {
        itens = List.copyOf(itens);
    }

    /** Soma dos produtos, arredondada para centavos. */
    public BigDecimal subtotal() {
        return Dinheiro.arredondar(itens.stream().map(ItemPedido::valor).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso total em kg, sem arredondamento. */
    public BigDecimal pesoKg() {
        return itens.stream().map(ItemPedido::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
