package com.loja.checkout.api;

import java.util.List;

/**
 * Dados da compra enviados pelo site. Os codigos chegam como texto para que um
 * valor desconhecido seja recusado com o codigo de erro correto, em vez de
 * quebrar a leitura do JSON.
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
