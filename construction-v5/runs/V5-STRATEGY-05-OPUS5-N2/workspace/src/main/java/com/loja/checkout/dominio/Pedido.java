package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** O carrinho ja conferido: so itens validos. */
public record Pedido(List<ItemPedido> itens) {

    public Pedido {
        itens = List.copyOf(itens);
    }

    /** Monta o pedido a partir do carrinho enviado pelo site, ou recusa. */
    public static Pedido conferido(List<ItemSolicitado> carrinho) {
        if (carrinho == null || carrinho.isEmpty()) {
            throw new PedidoRecusadoException(MotivoRecusa.PEDIDO_INVALIDO);
        }
        return new Pedido(carrinho.stream().map(Pedido::itemConferido).toList());
    }

    private static ItemPedido itemConferido(ItemSolicitado item) {
        if (item == null
                || !positivo(item.precoUnitario())
                || !positivo(item.pesoKg())
                || item.quantidade() == null
                || item.quantidade() <= 0) {
            throw new PedidoRecusadoException(MotivoRecusa.PEDIDO_INVALIDO);
        }
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal subtotalProdutos() {
        return Dinheiro.centavos(itens.stream()
                .map(ItemPedido::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso do pedido em kg, sem arredondar. */
    public BigDecimal pesoKg() {
        return itens.stream()
                .map(ItemPedido::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
