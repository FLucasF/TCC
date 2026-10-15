package com.loja.checkout.dominio.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** 2% dos produtos em credito para a proxima compra. */
@Component
public class ClubePrata implements NivelClube {

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal percentualCredito() {
        return new BigDecimal("2");
    }
}
