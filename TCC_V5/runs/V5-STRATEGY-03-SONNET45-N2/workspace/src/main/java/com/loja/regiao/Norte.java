package com.loja.regiao;

import java.math.BigDecimal;

public class Norte implements Regiao {

    @Override
    public BigDecimal percentualSeguro() {
        return new BigDecimal("0.025");
    }
}
