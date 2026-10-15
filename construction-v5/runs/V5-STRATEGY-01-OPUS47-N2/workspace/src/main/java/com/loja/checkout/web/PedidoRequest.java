package com.loja.checkout.web;

import java.math.BigDecimal;
import java.util.List;

public record PedidoRequest(
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
