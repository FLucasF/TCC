package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** O carrinho do cliente, com o que todo caso de calculo precisa saber dele. */
public record Pedido(List<ItemPedido> itens, Regiao regiao) {

    public Pedido(List<ItemPedido> itens, Regiao regiao) {
        this.itens = List.copyOf(itens);
        this.regiao = regiao;
    }

    public BigDecimal subtotalProdutos() {
        return Dinheiro.emCentavos(itens.stream()
                .map(ItemPedido::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso do pedido, soma dos pesos dos itens, sem arredondar. */
    public BigDecimal pesoTotalKg() {
        return itens.stream()
                .map(ItemPedido::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
