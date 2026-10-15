package com.loja.checkout.api;

import java.util.List;

/**
 * Corpo da chamada POST /checkout/resumo. Os codigos chegam como texto para que
 * um valor desconhecido caia na validacao e vire o erro certo, em vez de erro de leitura.
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
