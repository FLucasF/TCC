package com.loja.checkout.dominio;

import java.math.BigDecimal;

public record ResultadoEntrega(
    BigDecimal frete,
    int prazoEntregaDias
) {}
