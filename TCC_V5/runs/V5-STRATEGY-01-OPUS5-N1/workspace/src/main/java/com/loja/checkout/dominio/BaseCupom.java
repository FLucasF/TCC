package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Dados do pedido que os cupons usam para calcular o desconto. */
public record BaseCupom(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
