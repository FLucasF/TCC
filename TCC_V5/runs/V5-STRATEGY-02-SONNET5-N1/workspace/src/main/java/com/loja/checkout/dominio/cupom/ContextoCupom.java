package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Item;
import java.math.BigDecimal;
import java.util.List;

public record ContextoCupom(BigDecimal subtotalProdutos, BigDecimal frete, List<Item> itens) {
}
