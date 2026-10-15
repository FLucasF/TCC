package com.loja.checkout.dominio;

import java.math.BigDecimal;

public record ResultadoClube(
    BigDecimal creditoProximaCompra,
    boolean brinde,
    boolean freteGratis
) {}
