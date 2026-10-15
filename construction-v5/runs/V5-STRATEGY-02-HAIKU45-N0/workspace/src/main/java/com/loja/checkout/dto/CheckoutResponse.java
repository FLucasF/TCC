package com.loja.checkout.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CheckoutResponse(
    @JsonProperty("subtotalProdutos") BigDecimal subtotalProdutos,
    @JsonProperty("descontoCupom") BigDecimal descontoCupom,
    @JsonProperty("frete") BigDecimal frete,
    @JsonProperty("prazoEntregaDias") int prazoEntregaDias,
    @JsonProperty("seguro") BigDecimal seguro,
    @JsonProperty("ajustePagamento") BigDecimal ajustePagamento,
    @JsonProperty("totalFinal") BigDecimal totalFinal,
    @JsonProperty("parcelas") int parcelas,
    @JsonProperty("valorParcela") BigDecimal valorParcela,
    @JsonProperty("creditoProximaCompra") BigDecimal creditoProximaCompra,
    @JsonProperty("brinde") boolean brinde,
    @JsonProperty("erro") String erro
) {
    public static CheckoutResponse error(String erro) {
        return new CheckoutResponse(null, null, null, 0, null, null, null, 0, null, null, false, erro);
    }
}
