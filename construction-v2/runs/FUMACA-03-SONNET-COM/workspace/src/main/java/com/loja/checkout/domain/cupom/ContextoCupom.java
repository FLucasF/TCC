package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.Item;

import java.math.BigDecimal;
import java.util.List;

public record ContextoCupom(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
