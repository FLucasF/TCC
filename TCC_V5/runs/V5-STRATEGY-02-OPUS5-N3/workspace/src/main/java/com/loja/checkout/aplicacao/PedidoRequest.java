package com.loja.checkout.aplicacao;

import java.math.BigDecimal;
import java.util.List;

/**
 * O que o site envia. Os códigos chegam como texto de propósito: valor
 * desconhecido tem que virar o código de erro certo, e não erro de leitura
 * do JSON.
 */
public record PedidoRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    public record ItemRequest(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
    }
}
