package com.loja.checkout.aplicacao;

import java.util.List;

/** O que o site envia para o servico calcular o resumo. */
public record SolicitacaoResumo(
        List<ItemSolicitado> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    public int parcelasOuUma() {
        return parcelas == null ? 1 : parcelas;
    }
}
