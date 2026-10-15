package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Item;

import java.math.BigDecimal;
import java.util.List;

/** O que uma promocao pode olhar para decidir o desconto. */
public record ContextoCupom(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
