package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.ItemCalculo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tudo que algum cupom pode precisar para decidir e calcular seu desconto:
 * os itens (para promoções por unidade), o subtotal dos produtos e o frete
 * já definido (para o frete grátis). A assinatura atende o cupom mais exigente.
 */
public record CupomContexto(List<ItemCalculo> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
