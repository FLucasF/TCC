package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dados do pedido ja calculados a partir dos itens, usados pelas modalidades de
 * entrega, cupons e formas de pagamento para decidir disponibilidade e valores.
 */
public record PedidoContext(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal pesoTotalKg) {
}
