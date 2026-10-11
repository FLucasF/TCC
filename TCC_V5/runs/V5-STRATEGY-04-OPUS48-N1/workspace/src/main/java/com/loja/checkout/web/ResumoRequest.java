package com.loja.checkout.web;

import java.util.List;

/**
 * A compra que o site envia para /checkout/resumo. Os códigos (modalidade,
 * cupom, forma de pagamento, nível, região) chegam como texto para que a própria
 * validação decida o erro quando o valor não existe ou não vem.
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
