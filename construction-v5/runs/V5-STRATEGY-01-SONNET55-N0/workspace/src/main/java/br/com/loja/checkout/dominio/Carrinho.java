package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Carrinho(List<Item> itens) {

    public BigDecimal subtotal() {
        return Dinheiro.arredondar(itens.stream().map(Item::total).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso total do pedido, sem arredondar. */
    public BigDecimal pesoKg() {
        return itens.stream().map(Item::pesoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
