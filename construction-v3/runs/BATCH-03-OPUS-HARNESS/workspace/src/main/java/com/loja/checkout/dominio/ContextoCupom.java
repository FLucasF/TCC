package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Tudo que um cupom pode olhar para decidir o desconto. */
public record ContextoCupom(Pedido pedido, BigDecimal frete) {

    public List<Item> itens() {
        return pedido.itens();
    }

    public BigDecimal subtotalProdutos() {
        return pedido.subtotalProdutos();
    }
}
