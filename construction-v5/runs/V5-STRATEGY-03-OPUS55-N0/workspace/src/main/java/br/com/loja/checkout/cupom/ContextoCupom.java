package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/** O que um cupom pode consultar para decidir o desconto. */
public record ContextoCupom(Pedido pedido, BigDecimal frete) {
}
