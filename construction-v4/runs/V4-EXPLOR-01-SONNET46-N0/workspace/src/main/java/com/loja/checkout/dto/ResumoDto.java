package com.loja.checkout.dto;

public record ResumoDto(
        double subtotalProdutos,
        double descontoCupom,
        double frete,
        int prazoEntregaDias,
        double seguro,
        double ajustePagamento,
        double totalFinal,
        int parcelas,
        double valorParcela,
        double creditoProximaCompra,
        boolean brinde
) {}
