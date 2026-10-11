package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.ItemPedido;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tudo que um cupom pode precisar para decidir se vale e quanto desconta:
 * o subtotal dos produtos, o frete (já efetivo, zerado quando o clube isenta) e
 * os itens do carrinho. Reúne o que o caso mais exigente pede.
 */
public record ContextoCupom(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemPedido> itens) {
}
