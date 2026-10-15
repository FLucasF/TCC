package com.loja.checkout.web;

import java.util.List;

/**
 * Compra como o site envia. Os campos de código (modalidade, cupom, pagamento,
 * nível e região) chegam como texto para que a resolução controle o código de
 * erro certo na ordem certa. Cupom e parcelas podem não vir.
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
