package br.com.loja.pedidos.pedido;

import java.math.BigDecimal;

public record ItemPedido(String sku, int quantidade, BigDecimal precoUnitario) {
}
