package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public record ContextoCupom(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemPedido> itens) {
}
