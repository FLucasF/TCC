package com.loja.checkout.service.pagamento;

public record ResultadoPagamento(double totalFinal, double ajuste, int parcelas, double valorParcela) {}
