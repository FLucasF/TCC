package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

public record Ajuste(BigDecimal totalFinal, BigDecimal valorParcela, BigDecimal valor) {}
