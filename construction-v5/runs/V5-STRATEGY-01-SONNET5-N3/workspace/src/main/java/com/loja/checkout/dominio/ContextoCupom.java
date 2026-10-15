package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Dados que algum cupom pode precisar; nem todo cupom usa tudo. */
public record ContextoCupom(BigDecimal subtotalProdutos, List<Item> itens, BigDecimal frete) {
}
