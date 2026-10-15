package com.lojaroupas.dto;

import com.google.gson.annotations.SerializedName;

public class RespostaCheckout {
    @SerializedName("subtotalProdutos")
    public Double subtotalProdutos;

    @SerializedName("descontoCupom")
    public Double descontoCupom;

    @SerializedName("frete")
    public Double frete;

    @SerializedName("prazoEntregaDias")
    public Integer prazoEntregaDias;

    @SerializedName("seguro")
    public Double seguro;

    @SerializedName("ajustePagamento")
    public Double ajustePagamento;

    @SerializedName("totalFinal")
    public Double totalFinal;

    @SerializedName("parcelas")
    public Integer parcelas;

    @SerializedName("valorParcela")
    public Double valorParcela;

    @SerializedName("creditoProximaCompra")
    public Double creditoProximaCompra;

    @SerializedName("brinde")
    public Boolean brinde;

    public RespostaCheckout() {}

    public RespostaCheckout(Double subtotalProdutos, Double descontoCupom, Double frete,
                           Integer prazoEntregaDias, Double seguro, Double ajustePagamento,
                           Double totalFinal, Integer parcelas, Double valorParcela,
                           Double creditoProximaCompra, Boolean brinde) {
        this.subtotalProdutos = subtotalProdutos;
        this.descontoCupom = descontoCupom;
        this.frete = frete;
        this.prazoEntregaDias = prazoEntregaDias;
        this.seguro = seguro;
        this.ajustePagamento = ajustePagamento;
        this.totalFinal = totalFinal;
        this.parcelas = parcelas;
        this.valorParcela = valorParcela;
        this.creditoProximaCompra = creditoProximaCompra;
        this.brinde = brinde;
    }
}
