package com.loja.checkout.cupom;

import com.loja.checkout.pedido.ItemPedido;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tudo que algum cupom precisa para decidir aplicabilidade e desconto.
 * A assinatura atende o cupom mais exigente: itens (LEVE3PAGUE2), subtotal
 * (BEMVINDO10/MENOS50) e frete já exibido (FRETEGRATIS).
 */
public record CupomContexto(BigDecimal subtotal, BigDecimal frete, List<ItemPedido> itens) {
}
