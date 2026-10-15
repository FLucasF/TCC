package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** O carrinho do cliente, ja validado, com quem esta comprando. */
public record Pedido(List<ItemPedido> itens, NivelClube nivelClube, Regiao regiao) {

    public Pedido {
        itens = List.copyOf(itens);
    }

    /** Soma dos produtos. */
    public BigDecimal subtotalProdutos() {
        return Centavos.arredondar(itens.stream()
                .map(ItemPedido::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso do pedido: soma do peso de cada item vezes a quantidade, sem arredondar. */
    public BigDecimal pesoKg() {
        return itens.stream()
                .map(ItemPedido::pesoTotalKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
