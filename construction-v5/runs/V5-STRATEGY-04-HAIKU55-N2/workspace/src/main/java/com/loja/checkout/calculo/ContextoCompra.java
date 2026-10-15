package com.loja.checkout.calculo;

import java.math.BigDecimal;
import java.util.List;

public record ContextoCompra(List<ItemCarrinho> itens, BigDecimal subtotal, BigDecimal frete) {
}
