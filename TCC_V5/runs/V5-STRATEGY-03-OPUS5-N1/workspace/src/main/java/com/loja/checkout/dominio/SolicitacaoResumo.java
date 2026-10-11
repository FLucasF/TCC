package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Os dados da compra como o site envia: nada aqui é confiável ainda. */
public record SolicitacaoResumo(
        List<ItemRecebido> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    public record ItemRecebido(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
    }
}
