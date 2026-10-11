package com.loja.checkout.api.dto;

import java.util.List;

/**
 * Dados da compra enviados pelo site para /checkout/resumo. Os campos de
 * opcoes (entrega, cupom, pagamento, nivel, regiao) chegam como texto e sao
 * validados no servico, para que um valor desconhecido vire um codigo de erro
 * claro em vez de uma falha de leitura do JSON.
 */
public record CheckoutRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {
}
