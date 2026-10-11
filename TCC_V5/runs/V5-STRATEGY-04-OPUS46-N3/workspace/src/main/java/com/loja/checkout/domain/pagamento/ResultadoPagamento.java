package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {}
