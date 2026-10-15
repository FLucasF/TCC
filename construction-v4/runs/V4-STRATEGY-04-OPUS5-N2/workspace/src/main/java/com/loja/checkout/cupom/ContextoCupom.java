package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Item;
import java.math.BigDecimal;
import java.util.List;

/**
 * O que um cupom pode olhar para decidir o desconto. Reune tudo que as
 * promocoes de hoje usam, para uma promocao nova nao precisar mudar a
 * assinatura do desconto.
 */
public record ContextoCupom(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
