package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tudo o que um cupom pode precisar para decidir se vale e quanto desconta.
 * A assinatura atende o cupom mais exigente: itens (LEVE3PAGUE2), subtotal
 * (MENOS50) e frete do pedido (FRETEGRATIS).
 */
public record ContextoCupom(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
