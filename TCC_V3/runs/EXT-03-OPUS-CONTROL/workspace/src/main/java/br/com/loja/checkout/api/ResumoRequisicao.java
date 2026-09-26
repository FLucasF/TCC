package br.com.loja.checkout.api;

import java.util.List;

/** O corpo do POST /checkout/resumo. */
public record ResumoRequisicao(
        List<ItemRequisicao> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {
}
