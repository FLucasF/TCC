package com.loja.checkout.entrega;

import java.math.BigDecimal;

public record EntregaCalculada(BigDecimal valor, int prazoDias) {
}
