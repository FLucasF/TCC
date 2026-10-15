package com.loja.domain.cupom;

import java.math.BigDecimal;

public class ResultadoDesconto {
    private final BigDecimal desconto;

    public ResultadoDesconto(BigDecimal desconto) {
        this.desconto = desconto;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }
}
