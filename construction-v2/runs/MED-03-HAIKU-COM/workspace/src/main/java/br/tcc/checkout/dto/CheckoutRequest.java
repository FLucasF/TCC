package br.tcc.checkout.dto;

import java.util.List;

public record CheckoutRequest(
    List<Item> itens,
    String modalidadeEntrega,
    String cupom,
    String formaPagamento,
    Integer parcelas
) {}
