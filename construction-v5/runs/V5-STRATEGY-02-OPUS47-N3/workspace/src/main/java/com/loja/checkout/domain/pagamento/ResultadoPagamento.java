package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public record ResultadoPagamento(
        BigDecimal ajuste,
        BigDecimal totalFinal,
        int parcelas,
        BigDecimal valorParcela) {}
