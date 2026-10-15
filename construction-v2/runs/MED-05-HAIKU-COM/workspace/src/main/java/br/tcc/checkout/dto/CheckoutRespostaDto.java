package br.tcc.checkout.dto;

public record CheckoutRespostaDto(
    Double subtotalProdutos,
    Double descontoCupom,
    Double frete,
    Integer prazoEntregaDias,
    Double ajustePagamento,
    Double totalFinal,
    Integer parcelas,
    Double valorParcela
) {}
