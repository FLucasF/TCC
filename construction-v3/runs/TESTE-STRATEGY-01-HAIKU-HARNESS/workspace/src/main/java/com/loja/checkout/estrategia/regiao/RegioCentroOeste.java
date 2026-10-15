package com.loja.checkout.estrategia.regiao;

import java.math.BigDecimal;

public class RegioCentroOeste implements EstrategiaRegiao {
    @Override
    public BigDecimal getAliquota() {
        return new BigDecimal("0.09");
    }
}
