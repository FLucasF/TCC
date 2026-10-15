package com.loja.pedidos.api;

import java.math.BigDecimal;

public record NovoPedido(BigDecimal valorProdutos, BigDecimal frete) {
}
