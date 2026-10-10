package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public record ResultadoPagamento(
    BigDecimal ajuste,
    BigDecimal totalFinal,
    Integer parcelas,
    BigDecimal valorParcela
) {}
