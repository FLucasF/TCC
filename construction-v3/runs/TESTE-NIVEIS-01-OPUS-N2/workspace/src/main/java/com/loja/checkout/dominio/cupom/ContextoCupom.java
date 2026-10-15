package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import java.util.List;

/**
 * Tudo que um cupom pode precisar olhar: os itens do carrinho, o valor dos
 * produtos e o frete ja cobrado neste pedido.
 */
public record ContextoCupom(Pedido pedido, BigDecimal frete) {

    public List<Item> itens() {
        return pedido.itens();
    }

    public BigDecimal subtotalProdutos() {
        return pedido.subtotalProdutos();
    }
}
