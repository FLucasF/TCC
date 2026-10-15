package br.tcc.checkout.cupom;

import java.math.BigDecimal;

import br.tcc.checkout.dominio.Pedido;

/** Tudo que um cupom pode olhar para decidir o desconto: o carrinho e o frete já calculado. */
public record ContextoCupom(Pedido pedido, BigDecimal frete) {
}
