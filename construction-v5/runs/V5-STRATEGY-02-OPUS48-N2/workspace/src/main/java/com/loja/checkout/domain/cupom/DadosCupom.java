package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.ItemPedido;

import java.math.BigDecimal;
import java.util.List;

/**
 * O que um cupom precisa olhar para decidir o desconto: o subtotal dos produtos,
 * os itens (para regras por unidade) e o frete ja aplicado (para frete gratis).
 */
public record DadosCupom(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete) {
}
