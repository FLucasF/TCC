package com.loja.resumo;

import java.math.BigDecimal;
import java.util.List;

public record Contexto(List<ItemPedido> itens, BigDecimal subtotal, BigDecimal frete) {
}
