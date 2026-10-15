package com.loja.checkout.web;

import java.util.List;

/**
 * A compra como o site envia para /checkout/resumo. Nomes e formatos
 * seguem exatamente o combinado no anexo.
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
