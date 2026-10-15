package com.loja.checkout.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 2% dos produtos de volta em crédito. */
@Component
public class NivelPrata implements NivelClube {

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal percentualCredito() {
        return new BigDecimal("0.02");
    }
}
