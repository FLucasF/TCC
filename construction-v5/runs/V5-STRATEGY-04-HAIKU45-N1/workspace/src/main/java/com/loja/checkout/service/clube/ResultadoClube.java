package com.loja.checkout.service.clube;

import java.math.BigDecimal;

public record ResultadoClube(
    BigDecimal desconto,
    BigDecimal credito,
    boolean brinde
) {}
