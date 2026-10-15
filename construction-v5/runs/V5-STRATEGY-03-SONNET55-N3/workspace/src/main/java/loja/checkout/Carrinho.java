package loja.checkout;

import java.math.BigDecimal;
import java.util.List;

public record Carrinho(List<Item> itens) {
    public BigDecimal subtotal() {
        return itens.stream().map(Item::total).reduce(Dinheiro.ZERO, BigDecimal::add);
    }

    public BigDecimal pesoKg() {
        return itens.stream().map(Item::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
