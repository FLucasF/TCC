package com.loja.checkout.web;

import java.util.List;

/**
 * Pedido como o site envia. Códigos (modalidade, cupom, pagamento, nível,
 * região) ficam como texto para que valores desconhecidos virem o código de
 * erro certo, em vez de uma falha de desserialização. Cupom e parcelas podem
 * não vir.
 */
public record PedidoRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {
}
