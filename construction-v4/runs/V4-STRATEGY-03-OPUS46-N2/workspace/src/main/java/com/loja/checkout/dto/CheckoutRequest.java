package com.loja.checkout.dto;

import java.util.List;

public record CheckoutRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao
) {}
