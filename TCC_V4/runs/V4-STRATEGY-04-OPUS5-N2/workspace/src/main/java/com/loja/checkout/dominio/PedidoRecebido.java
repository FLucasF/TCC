package com.loja.checkout.dominio;

import java.util.List;
import java.util.Objects;

/** O pedido como o site envia, ainda sem validar. */
public record PedidoRecebido(
        List<ItemRecebido> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    public Carrinho carrinho() {
        ErroPedido.PEDIDO_INVALIDO.exigir(itens != null
                && !itens.isEmpty()
                && itens.stream().allMatch(Objects::nonNull));
        return new Carrinho(itens.stream().map(ItemRecebido::validado).toList());
    }

    /** Quando o site nao envia as parcelas, e uma. */
    public int parcelasEscolhidas() {
        return parcelas == null ? 1 : parcelas;
    }
}
