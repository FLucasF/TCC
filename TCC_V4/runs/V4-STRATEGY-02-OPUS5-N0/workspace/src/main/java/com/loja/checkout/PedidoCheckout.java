package com.loja.checkout;

import java.util.List;

/** Dados da compra como o site envia, ainda sem nenhuma validação. */
public record PedidoCheckout(List<ItemPedidoCheckout> itens,
                             String modalidadeEntrega,
                             String cupom,
                             String formaPagamento,
                             Integer parcelas,
                             String nivelClube,
                             String regiao) {
}
