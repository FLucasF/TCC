package com.loja.checkout;

import java.util.List;

public record PedidoSolicitado(
        List<Item> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    public record Item(String nome, java.math.BigDecimal precoUnitario, Integer quantidade, java.math.BigDecimal pesoKg) {
    }
}
