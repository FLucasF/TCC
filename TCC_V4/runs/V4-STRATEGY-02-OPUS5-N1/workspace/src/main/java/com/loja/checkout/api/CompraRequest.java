package com.loja.checkout.api;

import java.util.List;

/** Os dados da compra, como o site manda para /checkout/resumo. */
public record CompraRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    private static final int PARCELAS_PADRAO = 1;

    /** Quando o site nao manda o numero de parcelas, a compra e em 1 vez. */
    public int parcelasEscolhidas() {
        return parcelas == null ? PARCELAS_PADRAO : parcelas;
    }
}
