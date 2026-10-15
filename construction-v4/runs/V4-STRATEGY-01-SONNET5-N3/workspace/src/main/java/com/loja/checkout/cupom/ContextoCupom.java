package com.loja.checkout.cupom;

import com.loja.checkout.model.Item;
import java.math.BigDecimal;
import java.util.List;

public record ContextoCupom(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
