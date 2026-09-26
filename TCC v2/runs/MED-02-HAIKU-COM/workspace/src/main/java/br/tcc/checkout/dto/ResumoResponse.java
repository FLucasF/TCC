package br.tcc.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class ResumoResponse {
    @JsonProperty("subtotalProdutos")
    public BigDecimal subtotalProdutos;

    @JsonProperty("descontoCupom")
    public BigDecimal descontoCupom;

    @JsonProperty("frete")
    public BigDecimal frete;

    @JsonProperty("prazoEntregaDias")
    public Integer prazoEntregaDias;

    @JsonProperty("ajustePagamento")
    public BigDecimal ajustePagamento;

    @JsonProperty("totalFinal")
    public BigDecimal totalFinal;

    @JsonProperty("parcelas")
    public Integer parcelas;

    @JsonProperty("valorParcela")
    public BigDecimal valorParcela;

    public ResumoResponse(
            BigDecimal subtotalProdutos,
            BigDecimal descontoCupom,
            BigDecimal frete,
            Integer prazoEntregaDias,
            BigDecimal ajustePagamento,
            BigDecimal totalFinal,
            Integer parcelas,
            BigDecimal valorParcela) {
        this.subtotalProdutos = subtotalProdutos;
        this.descontoCupom = descontoCupom;
        this.frete = frete;
        this.prazoEntregaDias = prazoEntregaDias;
        this.ajustePagamento = ajustePagamento;
        this.totalFinal = totalFinal;
        this.parcelas = parcelas;
        this.valorParcela = valorParcela;
    }
}
