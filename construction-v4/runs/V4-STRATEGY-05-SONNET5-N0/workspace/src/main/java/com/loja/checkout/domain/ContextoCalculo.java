package com.loja.checkout.domain;

import com.loja.checkout.web.dto.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dados do pedido ja calculados ate o momento, usados pelos cupons para
 * decidir se se aplicam e calcular o desconto.
 */
public record ContextoCalculo(
        List<ItemRequest> itens,
        BigDecimal subtotalProdutos,
        BigDecimal frete
) {
}
