package com.loja.checkout.cupom;

import com.loja.checkout.domain.Pedido;

import java.math.BigDecimal;

public record ContextoCupom(Pedido pedido, BigDecimal subtotalProdutos, BigDecimal frete) {
}
