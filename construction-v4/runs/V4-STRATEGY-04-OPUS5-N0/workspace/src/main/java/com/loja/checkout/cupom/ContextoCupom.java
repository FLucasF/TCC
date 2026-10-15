package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import java.util.List;

/** O que um cupom pode olhar para decidir se vale e quanto desconta. */
public record ContextoCupom(Pedido pedido, BigDecimal subtotalProdutos, BigDecimal frete) {

    public List<Item> itens() {
        return pedido.itens();
    }
}
