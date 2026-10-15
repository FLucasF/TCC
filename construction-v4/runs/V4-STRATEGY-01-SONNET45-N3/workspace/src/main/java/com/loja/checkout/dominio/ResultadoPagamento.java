package com.loja.checkout.dominio;

import java.math.BigDecimal;

public record ResultadoPagamento(
    BigDecimal totalFinal,
    int parcelas,
    BigDecimal valorParcela,
    BigDecimal ajustePagamento
) {}
