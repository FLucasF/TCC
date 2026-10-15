package com.loja.checkout.cupom;

import com.loja.checkout.Item;

import java.math.BigDecimal;
import java.util.List;

public record CupomContexto(
        BigDecimal subtotalProdutos,
        BigDecimal frete,
        List<Item> itens) {
}
