package com.loja.regiao;

import java.math.BigDecimal;

public class CentroOeste implements Regiao {

    @Override
    public BigDecimal percentualSeguro() {
        return new BigDecimal("0.015");
    }
}
