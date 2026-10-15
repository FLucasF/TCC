package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Tudo de que algum cupom precisa para decidir se vale e quanto desconta. */
public record ContextoCupom(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
