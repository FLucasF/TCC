package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dados que os cupons precisam para decidir desconto e aplicabilidade. Reúne o
 * que o caso mais exigente pede: subtotal (MENOS50), frete (FRETEGRATIS) e os
 * itens (LEVE3PAGUE2). O frete já vem com a isenção do clube aplicada.
 */
public record ContextoCupom(BigDecimal subtotalProdutos, BigDecimal frete, List<Item> itens) {
}
