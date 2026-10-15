package com.loja.pedidos.api;

import java.math.BigDecimal;

/** Corpo de {@code POST /pedidos}. */
public record CriarPedidoRequisicao(BigDecimal valorProdutos, BigDecimal frete) {
}
