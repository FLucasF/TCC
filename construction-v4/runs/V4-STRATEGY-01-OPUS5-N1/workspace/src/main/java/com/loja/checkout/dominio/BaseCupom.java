package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Tudo de que um cupom pode precisar para decidir se vale e quanto desconta. */
public record BaseCupom(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
