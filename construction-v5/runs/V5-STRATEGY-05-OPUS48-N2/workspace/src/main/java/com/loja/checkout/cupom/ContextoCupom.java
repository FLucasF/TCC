package com.loja.checkout.cupom;

import com.loja.checkout.pedido.ItemPedido;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tudo que algum cupom pode precisar para decidir se vale e quanto desconta.
 * A assinatura atende o caso mais exigente: FRETEGRATIS precisa do frete e
 * LEVE3PAGUE2 precisa dos itens, então ambos entram no contexto.
 *
 * O frete aqui já é o frete final do resumo (depois de eventual isenção).
 */
public record ContextoCupom(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemPedido> itens) {
}
