package com.loja.checkout;

import java.math.BigDecimal;
import java.util.List;

record Pedido(List<ItemPedido> itens, BigDecimal subtotal, BigDecimal frete) {
}
