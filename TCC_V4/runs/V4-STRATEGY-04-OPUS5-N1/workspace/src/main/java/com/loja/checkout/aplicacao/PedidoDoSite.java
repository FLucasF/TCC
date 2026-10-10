package com.loja.checkout.aplicacao;

import java.math.BigDecimal;
import java.util.List;

/** Os dados da compra, exatamente como o site envia. */
public record PedidoDoSite(
        List<ItemDoSite> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    public record ItemDoSite(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
    }
}
