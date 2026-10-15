package com.loja.checkout.api;

import java.math.BigDecimal;
import java.util.List;

/** Corpo enviado pelo site em POST /checkout/resumo. */
public record ResumoRequisicao(
        List<ItemRequisicao> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas) {

    public record ItemRequisicao(
            String nome,
            BigDecimal precoUnitario,
            Integer quantidade,
            BigDecimal pesoKg) {
    }
}
