package com.loja.checkout.web;

import java.util.List;

/**
 * Os dados da compra, como o site envia para /checkout/resumo. Os codigos
 * (modalidade, cupom, pagamento, nivel, regiao) vem como texto livre para que um
 * valor desconhecido vire o erro certo, e nao uma falha de desserializacao.
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
