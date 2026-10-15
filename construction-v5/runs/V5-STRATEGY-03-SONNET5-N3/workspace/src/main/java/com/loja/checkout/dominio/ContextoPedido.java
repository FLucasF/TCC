package com.loja.checkout.dominio;

import com.loja.checkout.dto.ItemDTO;

import java.math.BigDecimal;
import java.util.List;

public record ContextoPedido(List<ItemDTO> itens, BigDecimal subtotalProdutos) {
}
