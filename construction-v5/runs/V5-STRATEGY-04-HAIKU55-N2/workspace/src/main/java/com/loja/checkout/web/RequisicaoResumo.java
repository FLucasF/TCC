package com.loja.checkout.web;

import java.util.List;

public record RequisicaoResumo(
        List<ItemRequisicao> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {
}
