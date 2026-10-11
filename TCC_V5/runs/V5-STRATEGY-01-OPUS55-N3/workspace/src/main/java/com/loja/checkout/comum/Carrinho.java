package com.loja.checkout.comum;

import java.math.BigDecimal;
import java.util.List;

public record Carrinho(List<Item> itens) {

    public Carrinho {
        itens = List.copyOf(itens);
    }

    public BigDecimal subtotal() {
        return Dinheiro.centavos(itens.stream().map(Item::valorTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso total, sem arredondar. */
    public BigDecimal pesoKg() {
        return itens.stream().map(Item::pesoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
