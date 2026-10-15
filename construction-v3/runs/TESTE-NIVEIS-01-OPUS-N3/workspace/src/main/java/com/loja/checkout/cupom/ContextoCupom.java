package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/**
 * O que os cupons de hoje precisam olhar: o pedido (itens e subtotal) e o
 * frete já apurado, de que o FRETEGRATIS depende.
 */
public record ContextoCupom(Pedido pedido, BigDecimal frete) {

    public BigDecimal subtotalProdutos() {
        return pedido.subtotal();
    }
}
