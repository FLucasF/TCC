package com.loja.regiao;

import java.math.BigDecimal;

public class Nordeste implements Regiao {

    @Override
    public BigDecimal percentualSeguro() {
        return new BigDecimal("0.02");
    }
}
