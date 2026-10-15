package com.loja.checkout.model;

public record ResumoResponse(
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
