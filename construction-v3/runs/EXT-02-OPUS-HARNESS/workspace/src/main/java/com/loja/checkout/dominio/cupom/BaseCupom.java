package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/** O que um cupom pode olhar: os itens, o valor dos produtos e o frete já aplicado. */
public record BaseCupom(Pedido pedido, BigDecimal subtotalProdutos, BigDecimal frete) {
}
