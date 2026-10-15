package com.loja.checkout.entrega;

import java.math.BigDecimal;

public record PedidoContexto(BigDecimal subtotalProdutos, BigDecimal pesoTotalKg) {
}
