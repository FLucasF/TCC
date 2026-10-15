package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Item;

import java.math.BigDecimal;
import java.util.List;

public record ContextoCupom(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal freteBase) {
}
