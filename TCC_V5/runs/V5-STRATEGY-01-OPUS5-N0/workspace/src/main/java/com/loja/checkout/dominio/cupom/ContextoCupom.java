package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.ItemPedido;
import java.math.BigDecimal;
import java.util.List;

/** Dados do pedido que os cupons podem usar para calcular o desconto. */
public record ContextoCupom(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
