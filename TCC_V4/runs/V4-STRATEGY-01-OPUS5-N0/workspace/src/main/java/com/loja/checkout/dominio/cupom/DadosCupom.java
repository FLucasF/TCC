package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

/** O que a promocao precisa saber do pedido para calcular o desconto. */
public record DadosCupom(List<ItemCarrinho> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
