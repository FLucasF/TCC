package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

public record Frete(BigDecimal valor, int prazoDias) {
}
