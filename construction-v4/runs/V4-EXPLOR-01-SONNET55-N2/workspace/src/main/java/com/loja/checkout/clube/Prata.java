package com.loja.checkout.clube;

import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Prata implements NivelClube {
    public String codigo() { return "PRATA"; }

    public BigDecimal frete(BigDecimal freteBase) { return freteBase; }

    public BigDecimal credito(BigDecimal subtotal) {
        return Dinheiro.arredondar(subtotal.multiply(new BigDecimal("0.02")));
    }

    public boolean brinde(BigDecimal subtotal) { return false; }
}
