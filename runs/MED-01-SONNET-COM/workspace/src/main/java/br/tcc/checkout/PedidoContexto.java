package br.tcc.checkout;

import java.math.BigDecimal;
import java.util.List;

record PedidoContexto(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal pesoTotalKg) {
}
