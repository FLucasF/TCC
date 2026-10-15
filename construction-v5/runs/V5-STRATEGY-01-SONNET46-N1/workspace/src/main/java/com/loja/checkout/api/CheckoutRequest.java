package com.loja.checkout.api;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao
) {
    public record ItemRequest(
            String nome,
            BigDecimal precoUnitario,
            Integer quantidade,
            BigDecimal pesoKg
    ) {}
}
