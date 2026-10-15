package com.loja.resumo.domain;

import java.math.BigDecimal;
import java.util.List;

public record ContextoCupom(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete) {
}
