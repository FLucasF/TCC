package com.loja.checkout.service.pagamento;

import java.math.BigDecimal;

public record ResultadoPagamento(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {}
