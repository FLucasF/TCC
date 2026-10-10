package com.loja.checkout.clube;

import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class NivelBronze implements NivelClube {
    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotal) {
        return Dinheiro.ZERO;
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
