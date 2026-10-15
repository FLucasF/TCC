package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.List;

public record PedidoContext(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
