package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.ItemPedido;
import java.math.BigDecimal;
import java.util.List;

/**
 * O que um cupom pode precisar para decidir se vale e quanto desconta.
 * A assinatura atende o caso mais exigente: FRETEGRATIS precisa do frete e
 * LEVE3PAGUE2 precisa dos itens, então ambos estão aqui.
 */
public record ContextoCupom(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete) {
}
