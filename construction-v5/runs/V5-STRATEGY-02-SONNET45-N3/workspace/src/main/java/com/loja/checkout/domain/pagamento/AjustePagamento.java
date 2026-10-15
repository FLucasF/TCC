package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public record AjustePagamento(
    BigDecimal ajuste,
    BigDecimal totalFinal,
    BigDecimal valorParcela
) {}
