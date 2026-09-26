package com.loja.checkout.dominio;

import com.loja.checkout.dominio.clube.NivelClube;
import java.math.BigDecimal;
import java.util.List;

/** Carrinho do cliente: os itens e quem esta comprando. */
public record Pedido(List<Item> itens, NivelClube nivelClube, Regiao regiao) {

    /** Soma dos produtos, arredondada em centavos. */
    public BigDecimal subtotalProdutos() {
        return Moeda.centavos(itens.stream()
                .map(Item::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso do pedido em kg, sem arredondamento. */
    public BigDecimal pesoKg() {
        return itens.stream()
                .map(Item::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
