package com.loja.resumo;

import java.math.BigDecimal;
import java.util.List;

public record ResumoRequisicao(
        List<ItemRequisicao> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    public record ItemRequisicao(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
    }
}
