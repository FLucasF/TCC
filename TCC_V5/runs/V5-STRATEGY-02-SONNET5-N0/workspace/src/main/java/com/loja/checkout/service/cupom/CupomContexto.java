package com.loja.checkout.service.cupom;

import com.loja.checkout.web.dto.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public record CupomContexto(
        List<ItemRequest> itens,
        BigDecimal subtotalProdutos,
        BigDecimal freteCalculado
) {
}
