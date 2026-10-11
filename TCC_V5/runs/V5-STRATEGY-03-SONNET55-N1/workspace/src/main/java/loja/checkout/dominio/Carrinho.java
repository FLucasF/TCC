package loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Carrinho(List<Item> itens) {

    public BigDecimal subtotal() {
        return itens.stream().map(Item::subtotal).reduce(Dinheiro.ZERO, BigDecimal::add);
    }

    public BigDecimal pesoKg() {
        return itens.stream().map(Item::pesoTotalKg).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
