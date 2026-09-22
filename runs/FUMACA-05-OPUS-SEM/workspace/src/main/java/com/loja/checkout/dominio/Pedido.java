package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Carrinho validado: itens, subtotal em centavos e peso total sem arredondamento. */
public record Pedido(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal pesoKg) {

    public static Pedido de(List<ItemPedido> itens) {
        BigDecimal subtotal = itens.stream()
                .map(ItemPedido::valorBruto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal peso = itens.stream()
                .map(ItemPedido::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Pedido(List.copyOf(itens), Dinheiro.centavos(subtotal), peso);
    }
}
