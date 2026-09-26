package com.loja.checkout;

import java.math.BigDecimal;
import java.util.List;

public record PedidoContext(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal pesoTotalKg) {

    public record ItemPedido(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {
    }
}
