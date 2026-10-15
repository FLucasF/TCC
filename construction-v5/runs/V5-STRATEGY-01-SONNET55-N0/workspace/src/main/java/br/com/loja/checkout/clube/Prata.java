package br.com.loja.checkout.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Prata implements NivelClube {

    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal taxaCredito() {
        return new BigDecimal("0.02");
    }
}
