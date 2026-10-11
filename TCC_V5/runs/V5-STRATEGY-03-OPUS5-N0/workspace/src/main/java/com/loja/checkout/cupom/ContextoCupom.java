package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/**
 * O que o cupom pode olhar para decidir o desconto: o carrinho, o valor dos
 * produtos e o frete que ja foi calculado para este pedido.
 */
public record ContextoCupom(Pedido pedido, BigDecimal subtotalProdutos, BigDecimal frete) {
}
