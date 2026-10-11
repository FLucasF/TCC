package br.com.loja.checkout.dominio.cupom;

import java.math.BigDecimal;

import br.com.loja.checkout.dominio.Pedido;

/** O que um cupom pode olhar para decidir se vale e quanto desconta. */
public record ContextoCupom(Pedido pedido, BigDecimal frete) {

    public BigDecimal subtotalProdutos() {
        return pedido.subtotal();
    }
}
