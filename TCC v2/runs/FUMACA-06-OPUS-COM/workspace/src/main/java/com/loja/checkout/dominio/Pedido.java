package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

public record Pedido(List<Item> itens) {

    public Pedido {
        if (itens == null || itens.isEmpty() || !itens.stream().allMatch(item -> item != null && item.valido())) {
            throw new ErroCheckout(CodigoErro.PEDIDO_INVALIDO);
        }
        itens = List.copyOf(itens);
    }

    public BigDecimal subtotalProdutos() {
        return Dinheiro.emCentavos(somar(Item::total));
    }

    /** O peso do pedido nao e arredondado. */
    public BigDecimal pesoKg() {
        return somar(Item::peso);
    }

    private BigDecimal somar(Function<Item, BigDecimal> parcela) {
        return itens.stream().map(parcela).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
