package com.loja.checkout.clube;

import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Bronze implements NivelClube {
    public String codigo() { return "BRONZE"; }

    public BigDecimal frete(BigDecimal freteBase) { return freteBase; }

    public BigDecimal credito(BigDecimal subtotal) { return Dinheiro.arredondar(BigDecimal.ZERO); }

    public boolean brinde(BigDecimal subtotal) { return false; }
}
