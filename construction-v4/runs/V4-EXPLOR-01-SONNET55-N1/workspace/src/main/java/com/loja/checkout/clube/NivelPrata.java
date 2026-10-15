package com.loja.checkout.clube;

import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class NivelPrata implements NivelClube {
    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotal) {
        return Dinheiro.arredondar(subtotal.multiply(new BigDecimal("0.02")));
    }

    @Override
    public BigDecimal frete(BigDecimal freteDaEntrega) {
        return freteDaEntrega;
    }

    @Override
    public boolean brinde(BigDecimal subtotal) {
        return false;
    }
}
