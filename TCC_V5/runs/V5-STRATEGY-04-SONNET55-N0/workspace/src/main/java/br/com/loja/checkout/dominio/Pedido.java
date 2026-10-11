package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Pedido(List<Item> itens) {

    public BigDecimal subtotal() {
        return Dinheiro.arredondar(itens.stream().map(Item::total).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso total sem arredondamento. */
    public BigDecimal pesoKg() {
        return itens.stream().map(Item::pesoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
