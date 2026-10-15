package com.loja.checkout.dominio;

import java.util.List;

/** Os dados da compra, como o site envia (qualquer campo pode faltar). */
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
