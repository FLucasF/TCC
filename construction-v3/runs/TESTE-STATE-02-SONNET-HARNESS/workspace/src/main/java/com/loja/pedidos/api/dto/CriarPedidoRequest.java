package com.loja.pedidos.api.dto;

import java.math.BigDecimal;

public record CriarPedidoRequest(BigDecimal valorProdutos, BigDecimal frete) {
}
