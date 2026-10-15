package com.loja.checkout.pedido;

import java.util.List;

import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.ModalidadeEntrega;

public record Pedido(
        List<Item> itens,
        ModalidadeEntrega modalidadeEntrega,
        Cupom cupom,
        String formaPagamento,
        int parcelas,
        NivelClube nivelClube,
        Regiao regiao) {
}
