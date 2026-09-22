package com.loja.checkout.dto;

import java.util.List;

public record CheckoutRequest(
        List<ItemPedido> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas
) {

    public int parcelasOuPadrao() {
        return parcelas == null ? 1 : parcelas;
    }
}
