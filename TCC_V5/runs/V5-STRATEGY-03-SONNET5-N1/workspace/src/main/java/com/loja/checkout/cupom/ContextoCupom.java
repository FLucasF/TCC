package com.loja.checkout.cupom;

import com.loja.checkout.dominio.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

/**
 * Dados do pedido que um cupom pode precisar para decidir se vale e para
 * calcular o desconto. O frete entra aqui porque o FRETEGRATIS precisa do
 * valor do frete ja calculado (ja considerando o frete gratis do clube OURO).
 */
public record ContextoCupom(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
}
