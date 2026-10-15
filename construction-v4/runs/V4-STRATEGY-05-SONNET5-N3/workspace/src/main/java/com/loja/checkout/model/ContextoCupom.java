package com.loja.checkout.model;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public record ContextoCupom(
        BigDecimal subtotalProdutos,
        BigDecimal frete,
        List<ItemRequest> itens
) {
}
