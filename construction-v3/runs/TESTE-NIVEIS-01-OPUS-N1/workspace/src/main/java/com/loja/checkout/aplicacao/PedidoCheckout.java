package com.loja.checkout.aplicacao;

import java.math.BigDecimal;
import java.util.List;

/** Os dados da compra, como o site envia. Qualquer campo pode vir faltando. */
public record PedidoCheckout(
        List<ItemPedido> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    public record ItemPedido(
            String nome,
            BigDecimal precoUnitario,
            Integer quantidade,
            BigDecimal pesoKg) {
    }
}
