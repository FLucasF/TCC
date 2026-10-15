package com.loja.checkout.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Ganha 2% dos produtos em credito para a proxima compra. */
@Component
public class ClubePrata implements NivelClube {

    private static final BigDecimal CREDITO = new BigDecimal("0.02");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal percentualCredito() {
        return CREDITO;
    }
}
