package com.loja.checkout.web;

import java.util.List;

/**
 * Os dados da compra como chegam em /checkout/resumo. Os campos de escolha
 * (modalidade, cupom, forma de pagamento, nível, região) chegam como texto para
 * que um valor inexistente vire o erro certo em vez de falha de formato.
 * {@code cupom} e {@code parcelas} podem não vir.
 */
public record ResumoRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {
}
