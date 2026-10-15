package com.loja.checkout.estrategia.regiao;

import java.math.BigDecimal;

public class RegioNorte implements EstrategiaRegiao {
    @Override
    public BigDecimal getAliquota() {
        return new BigDecimal("0.07");
    }
}
