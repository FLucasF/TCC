package com.loja.checkout.entrega;

import java.math.BigDecimal;

public record CalculoFrete(BigDecimal valor, int prazoDias) {
}
