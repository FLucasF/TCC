package com.loja.checkout.coupon;

import com.loja.checkout.model.Item;

import java.math.BigDecimal;
import java.util.List;

public record CupomContexto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
