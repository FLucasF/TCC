package com.loja.pedidos.web;

import java.math.BigDecimal;

/** Corpo de POST /pedidos. */
public record CriarPedidoRequisicao(BigDecimal valorProdutos, BigDecimal frete) {
}
