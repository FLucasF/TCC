package com.loja.checkout.service.cupom;

import com.loja.checkout.dto.ItemRequest;
import java.math.BigDecimal;
import java.util.List;

public record ContextoCupom(
        List<ItemRequest> itens,
        BigDecimal subtotalProdutos,
        BigDecimal freteAtual
) {
}
