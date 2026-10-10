package com.loja.checkout.service;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public record PedidoContext(
        List<ItemRequest> itens,
        BigDecimal subtotalProdutos,
        BigDecimal pesoTotalKg
) {
}
