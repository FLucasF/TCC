package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/**
 * O que os cupons precisam conhecer do pedido. Reúne tudo de que o cupom mais
 * exigente precisa: os itens (LEVE3PAGUE2), o subtotal (BEMVINDO10, MENOS50) e
 * o frete do resumo (FRETEGRATIS).
 */
public record ContextoCupom(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
