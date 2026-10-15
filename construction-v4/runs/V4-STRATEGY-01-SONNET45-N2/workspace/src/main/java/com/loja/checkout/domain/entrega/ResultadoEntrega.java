package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

public record ResultadoEntrega(BigDecimal frete, Integer prazoEntregaDias) {}
