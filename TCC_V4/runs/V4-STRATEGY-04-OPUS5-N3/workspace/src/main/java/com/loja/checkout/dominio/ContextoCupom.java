package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/**
 * O que um cupom pode olhar para decidir o desconto. Atende o cupom mais exigente:
 * LEVE3PAGUE2 precisa dos itens, MENOS50 do subtotal e FRETEGRATIS do frete.
 *
 * <p>O frete aqui é o frete que vai aparecer no resumo, já com a isenção do clube. Então
 * FRETEGRATIS em cliente OURO desconta R$ 0,00: o cliente já não estava pagando frete.
 */
public record ContextoCupom(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
