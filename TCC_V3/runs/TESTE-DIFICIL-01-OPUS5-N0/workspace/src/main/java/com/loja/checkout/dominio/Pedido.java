package com.loja.checkout.dominio;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.regiao.Regiao;
import java.math.BigDecimal;
import java.util.List;

/**
 * O pedido do cliente depois de conferido: itens, quanto deu em produtos,
 * quanto pesa, o nivel do clube e a regiao.
 */
public record Pedido(List<Item> itens, NivelClube nivelClube, Regiao regiao) {

    /** Soma dos produtos (preco de cada item x quantidade). */
    public BigDecimal subtotalProdutos() {
        return Dinheiro.centavos(itens.stream()
                .map(Item::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso do pedido, somando peso x quantidade de cada item, sem arredondar. */
    public BigDecimal pesoKg() {
        return itens.stream()
                .map(Item::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
