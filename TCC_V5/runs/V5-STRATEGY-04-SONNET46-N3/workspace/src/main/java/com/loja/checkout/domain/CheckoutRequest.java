package com.loja.checkout.domain;

import java.util.List;

public record CheckoutRequest(
        List<CartItem> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao
) {}
