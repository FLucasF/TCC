package br.com.loja.checkout.carrinho;

import br.com.loja.checkout.Dinheiro;

import java.math.BigDecimal;
import java.util.List;

public record Carrinho(List<Item> itens) {

    public Carrinho {
        itens = List.copyOf(itens);
    }

    public BigDecimal subtotal() {
        return Dinheiro.centavos(itens.stream().map(Item::total).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso do pedido, sem arredondar. */
    public BigDecimal pesoKg() {
        return itens.stream().map(Item::pesoTotalKg).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
