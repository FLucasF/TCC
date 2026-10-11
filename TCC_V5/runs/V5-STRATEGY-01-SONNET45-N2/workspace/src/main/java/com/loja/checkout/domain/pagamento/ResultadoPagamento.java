package com.loja.checkout.domain.pagamento;

public record ResultadoPagamento(
    double ajuste,
    double totalFinal,
    double valorParcela
) {}
