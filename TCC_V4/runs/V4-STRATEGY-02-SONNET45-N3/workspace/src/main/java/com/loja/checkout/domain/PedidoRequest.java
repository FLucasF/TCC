package com.loja.checkout.domain;

import java.util.List;

public record PedidoRequest(
    List<ItemCarrinho> itens,
    String modalidadeEntrega,
    String cupom,
    String formaPagamento,
    Integer parcelas,
    String nivelClube,
    String regiao
) {
    public record ItemCarrinho(
        String nome,
        Double precoUnitario,
        Integer quantidade,
        Double pesoKg
    ) {}
}
