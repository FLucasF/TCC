package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

public record Pagamento(BigDecimal totalFinal, BigDecimal valorParcela) {}
