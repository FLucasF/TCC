package com.loja.checkout.api;

import java.util.List;

/**
 * O que o site envia. Os codigos chegam como texto para que um valor
 * desconhecido seja recusado com o codigo de erro certo, e nao como
 * requisicao malformada.
 */
public record ResumoRequisicao(
        List<ItemRequisicao> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {
}
