package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;
import java.util.List;

/** O que um cupom pode olhar para decidir o desconto. */
public record ContextoCupom(Pedido pedido, BigDecimal frete) {

    public BigDecimal subtotalProdutos() {
        return pedido.subtotalProdutos();
    }

    public List<ItemPedido> itens() {
        return pedido.itens();
    }
}
