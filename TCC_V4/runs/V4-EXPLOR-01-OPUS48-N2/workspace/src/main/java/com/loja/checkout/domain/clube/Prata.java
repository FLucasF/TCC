package com.loja.checkout.domain.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Ganha 2% dos produtos de volta em crédito. */
@Component
public class Prata implements NivelClube {

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal percentualCredito() {
        return new BigDecimal("0.02");
    }
}
