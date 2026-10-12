package br.com.loja.checkout;

import java.math.BigDecimal;
import java.util.List;

public record Carrinho(List<Item> itens) {

    public BigDecimal subtotal() {
        return Dinheiro.centavos(itens.stream().map(Item::valor).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso total, sem arredondar. */
    public BigDecimal pesoKg() {
        return itens.stream().map(Item::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
