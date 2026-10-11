package com.loja.checkout.clube;

import java.math.BigDecimal;

public record BeneficiosClube(
        BigDecimal creditoProximaCompra,
        boolean freteGratis,
        boolean brinde
) {}
