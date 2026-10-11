package com.loja.checkout;

import com.loja.checkout.dominio.Carrinho;

/** Pedido já com o carrinho validado; os demais campos são os códigos enviados pelo site. */
public record PedidoEntrada(
        Carrinho carrinho,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {
}
