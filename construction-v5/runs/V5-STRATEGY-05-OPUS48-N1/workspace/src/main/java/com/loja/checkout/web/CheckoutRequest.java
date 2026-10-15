package com.loja.checkout.web;

import java.util.List;

/**
 * Os dados da compra como o site envia para /checkout/resumo. Os codigos
 * (entrega, cupom, pagamento, clube, regiao) chegam como texto para que a
 * validacao devolva o codigo de erro certo em vez de falhar na leitura do JSON.
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
