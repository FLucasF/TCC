package com.loja.checkout.dto;

import java.util.List;

public record PedidoRequest(
    List<Item> itens,
    String modalidadeEntrega,
    String cupom,
    String formaPagamento,
    Integer parcelas,
    String nivelClube,
    String regiao
) {
    public record Item(
        String nome,
        Number precoUnitario,
        Integer quantidade,
        Number pesoKg
    ) {
    }
}
