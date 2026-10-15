package com.loja.checkout.service.cupom;

import java.math.BigDecimal;

public record ResultadoCupom(
    BigDecimal desconto,
    BigDecimal freteDesconto
) {}
