package com.loja.checkout.api;

import java.util.List;

/**
 * Os dados da compra como o site envia. Os codigos chegam como texto para que
 * um valor desconhecido seja recusado com o codigo de erro correto, e nao como
 * falha de leitura do JSON.
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
