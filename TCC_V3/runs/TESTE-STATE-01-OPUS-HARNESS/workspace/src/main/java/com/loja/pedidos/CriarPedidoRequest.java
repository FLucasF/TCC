package com.loja.pedidos;

import java.math.BigDecimal;

public record CriarPedidoRequest(BigDecimal valorProdutos, BigDecimal frete) {
}
