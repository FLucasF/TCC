package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** O que um cupom pode olhar para decidir se vale e quanto desconta. */
public record BaseCupom(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
