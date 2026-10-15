package com.loja.checkout.api.dto;

import java.util.List;

public record CheckoutRequestDto(
        List<ItemDto> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas
) {
}
