package com.loja.checkout.calculo;

import java.math.BigDecimal;
import java.util.List;

record Totais(List<ItemPedido> itens, BigDecimal subtotal, BigDecimal frete) {
}
