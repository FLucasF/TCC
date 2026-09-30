package com.loja.pedidos.api;

import java.math.BigDecimal;

public record CriarPedidoRequest(BigDecimal valorProdutos, BigDecimal frete) {
}
