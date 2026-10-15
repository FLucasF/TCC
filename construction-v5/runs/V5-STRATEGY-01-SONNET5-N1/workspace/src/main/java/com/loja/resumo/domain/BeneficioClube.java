package com.loja.resumo.domain;

import java.math.BigDecimal;

public record BeneficioClube(BigDecimal credito, boolean freteGratis, boolean brinde) {
}
