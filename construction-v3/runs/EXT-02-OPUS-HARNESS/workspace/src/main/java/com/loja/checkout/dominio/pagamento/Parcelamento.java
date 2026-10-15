package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

public record Parcelamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
