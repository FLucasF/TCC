package com.loja.checkout.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dados do pedido que um cupom pode precisar para validar a condição ou calcular o desconto.
 */
public record ContextoCupom(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal freteCalculado) {
}
