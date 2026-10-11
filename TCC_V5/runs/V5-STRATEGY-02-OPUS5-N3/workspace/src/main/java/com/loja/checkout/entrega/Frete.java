package com.loja.checkout.entrega;

import java.math.BigDecimal;

public record Frete(BigDecimal valor, int prazoDias) {
}
