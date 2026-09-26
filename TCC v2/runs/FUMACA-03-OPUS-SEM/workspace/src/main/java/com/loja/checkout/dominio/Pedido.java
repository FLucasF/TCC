package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Carrinho validado: itens, subtotal dos produtos e peso total. */
public record Pedido(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal pesoKg) {

    public static Pedido de(List<ItemPedido> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            subtotal = subtotal.add(item.totalBruto());
            peso = peso.add(item.pesoTotal());
        }
        return new Pedido(List.copyOf(itens), Dinheiro.centavos(subtotal), peso);
    }
}
