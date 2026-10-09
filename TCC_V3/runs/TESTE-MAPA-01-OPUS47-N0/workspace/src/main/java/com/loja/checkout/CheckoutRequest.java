package com.loja.checkout;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutRequest(
        List<Item> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao
) {
    public record Item(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {}
}
