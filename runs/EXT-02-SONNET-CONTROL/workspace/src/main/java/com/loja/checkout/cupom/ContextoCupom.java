package com.loja.checkout.cupom;

import com.loja.checkout.domain.ItemPedido;

import java.math.BigDecimal;
import java.util.List;

public record ContextoCupom(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal freteAntesDoCupom) {
}
