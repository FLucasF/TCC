package com.loja.checkout.api;

import java.math.BigDecimal;
import java.util.List;

/**
 * O que o site envia. Os códigos chegam como texto para que um valor que não
 * existe vire o código de erro da tabela, e não erro de leitura do JSON.
 */
public record ResumoRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    public record ItemRequest(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
    }

    /** Quando não vem, é 1. */
    public int parcelasOuUma() {
        return parcelas == null ? 1 : parcelas;
    }
}
