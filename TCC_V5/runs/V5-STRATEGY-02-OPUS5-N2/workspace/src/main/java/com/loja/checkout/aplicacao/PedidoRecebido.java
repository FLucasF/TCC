package com.loja.checkout.aplicacao;

import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.PedidoRecusado;

import java.util.ArrayList;
import java.util.List;

/** O pedido como o site manda: ainda sem conferir nada. */
public record PedidoRecebido(
        List<ItemRecebido> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    public Carrinho carrinho() {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoRecusado(CodigoErro.PEDIDO_INVALIDO);
        }
        List<Item> convertidos = new ArrayList<>();
        for (ItemRecebido item : itens) {
            if (item == null) {
                throw new PedidoRecusado(CodigoErro.PEDIDO_INVALIDO);
            }
            convertidos.add(item.paraItem());
        }
        return new Carrinho(convertidos);
    }

    public int parcelasEscolhidas() {
        return parcelas == null ? 1 : parcelas;
    }
}
