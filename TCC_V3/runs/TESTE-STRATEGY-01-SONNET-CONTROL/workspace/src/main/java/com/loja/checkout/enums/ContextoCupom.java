package com.loja.checkout.enums;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

/** Dados do pedido necessários para calcular o desconto de um cupom. */
public record ContextoCupom(
        List<ItemRequest> itens,
        BigDecimal subtotalProdutos,
        BigDecimal frete
) {
}
