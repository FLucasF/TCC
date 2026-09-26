package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public record DadosPedido(
        List<ItemRequest> itens,
        BigDecimal subtotalProdutos,
        BigDecimal frete
) {
}
