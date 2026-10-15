package br.loja.checkout;

import java.math.BigDecimal;
import java.util.List;

public record Pedido(List<ItemCarrinho> itens) {
    public BigDecimal subtotal() {
        return itens.stream().map(ItemCarrinho::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal pesoKg() {
        return itens.stream()
                .map(i -> i.pesoKg().multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
