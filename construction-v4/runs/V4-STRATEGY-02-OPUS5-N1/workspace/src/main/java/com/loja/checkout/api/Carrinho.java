package com.loja.checkout.api;

import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.erro.Codigo;
import com.loja.checkout.erro.PedidoRecusado;

import java.math.BigDecimal;
import java.util.List;

/**
 * Monta o pedido a partir do carrinho que o site mandou. Recusa carrinho vazio
 * e item com preco, quantidade ou peso zero, negativo ou ausente.
 */
final class Carrinho {

    private Carrinho() {
    }

    static Pedido montarPedido(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoRecusado(Codigo.PEDIDO_INVALIDO);
        }
        return new Pedido(itens.stream().map(Carrinho::montarItem).toList());
    }

    private static Item montarItem(ItemRequest item) {
        if (item == null) {
            throw new PedidoRecusado(Codigo.PEDIDO_INVALIDO);
        }
        return new Item(
                item.nome(),
                exigirPositivo(item.precoUnitario()),
                exigirPositivo(item.quantidade() == null ? null : BigDecimal.valueOf(item.quantidade())).intValue(),
                exigirPositivo(item.pesoKg()));
    }

    private static BigDecimal exigirPositivo(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PedidoRecusado(Codigo.PEDIDO_INVALIDO);
        }
        return valor;
    }
}
