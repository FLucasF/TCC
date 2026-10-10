package com.loja.checkout.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;

public record ResumoResponse(
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    BigDecimal subtotalProdutos,

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    BigDecimal descontoCupom,

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    BigDecimal frete,

    int prazoEntregaDias,

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    BigDecimal seguro,

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    BigDecimal ajustePagamento,

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    BigDecimal totalFinal,

    int parcelas,

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    BigDecimal valorParcela,

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    BigDecimal creditoProximaCompra,

    boolean brinde
) {}
