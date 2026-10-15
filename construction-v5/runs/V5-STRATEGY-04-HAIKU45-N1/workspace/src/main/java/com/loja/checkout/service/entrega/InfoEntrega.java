package com.loja.checkout.service.entrega;

import java.math.BigDecimal;

public record InfoEntrega(
    BigDecimal frete,
    int prazo
) {}
