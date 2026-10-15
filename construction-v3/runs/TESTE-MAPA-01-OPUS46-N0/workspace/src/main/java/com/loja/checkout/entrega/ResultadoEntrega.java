package com.loja.checkout.entrega;

import java.math.BigDecimal;

public record ResultadoEntrega(BigDecimal frete, int prazoDias) {
}
