package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.pedido.ItemPedido;

import java.math.BigDecimal;
import java.util.List;

public record ContextoDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemPedido> itens) {
}
