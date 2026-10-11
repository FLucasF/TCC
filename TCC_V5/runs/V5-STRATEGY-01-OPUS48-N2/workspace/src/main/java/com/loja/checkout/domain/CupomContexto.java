package com.loja.checkout.domain;

import com.loja.checkout.web.ItemPedido;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tudo o que um cupom pode precisar para decidir se aplica e quanto desconta.
 * A assinatura do cupom foi desenhada para o caso mais exigente: FRETEGRATIS
 * precisa do frete, LEVE3PAGUE2 precisa dos itens, MENOS50 precisa do subtotal.
 */
public record CupomContexto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemPedido> itens) {
}
