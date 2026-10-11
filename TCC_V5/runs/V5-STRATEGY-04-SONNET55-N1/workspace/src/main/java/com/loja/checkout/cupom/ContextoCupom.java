package com.loja.checkout.cupom;

import com.loja.checkout.Item;
import java.math.BigDecimal;
import java.util.List;

/** O que um cupom pode olhar no pedido: itens, valor dos produtos e frete já definido. */
public record ContextoCupom(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
