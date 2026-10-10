package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

/** O que a transportadora precisa saber do pedido para cobrar o frete. */
public record DadosEntrega(BigDecimal pesoKg, BigDecimal subtotalProdutos) {
}
