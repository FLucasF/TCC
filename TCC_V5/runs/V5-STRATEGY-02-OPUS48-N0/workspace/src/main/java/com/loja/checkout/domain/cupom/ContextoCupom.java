package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.Item;
import java.math.BigDecimal;
import java.util.List;

/**
 * Dados que um cupom pode precisar para decidir se vale e quanto desconta.
 * O frete aqui ja e o frete efetivo mostrado no resumo (usado pelo
 * FRETEGRATIS).
 */
public record ContextoCupom(BigDecimal subtotalProdutos, List<Item> itens, BigDecimal frete) {
}
