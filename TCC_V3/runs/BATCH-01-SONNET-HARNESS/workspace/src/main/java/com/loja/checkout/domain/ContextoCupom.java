package com.loja.checkout.domain;

import com.loja.checkout.dto.ItemPedidoDto;

import java.math.BigDecimal;
import java.util.List;

public record ContextoCupom(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemPedidoDto> itens) {
}
