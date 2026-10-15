package com.loja.resumo.dominio;

import java.math.BigDecimal;

public record ResultadoPagamento(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {
}
