package com.loja.checkout.cupom;

import com.loja.checkout.domain.Pedido;
import java.math.BigDecimal;

/**
 * O que um cupom pode olhar para decidir o desconto: o pedido e o frete
 * que ja apareceria no resumo.
 */
public record ContextoCupom(Pedido pedido, BigDecimal frete) {

    public BigDecimal subtotalProdutos() {
        return pedido.subtotalProdutos();
    }
}
