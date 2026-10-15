package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dados do pedido necessarios para avaliar e calcular um cupom.
 */
public record CupomContexto(BigDecimal subtotalProdutos, List<Item> itens, BigDecimal freteBruto) {
}
