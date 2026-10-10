package com.loja.checkout.clube;

import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class NivelOuro implements NivelClube {
    private static final BigDecimal LIMITE_BRINDE = Dinheiro.valor("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotal) {
        return Dinheiro.arredondar(subtotal.multiply(new BigDecimal("0.05")));
    }

    @Override
    public BigDecimal frete(BigDecimal freteDaEntrega) {
        return Dinheiro.ZERO;
    }

    @Override
    public boolean brinde(BigDecimal subtotal) {
        return subtotal.compareTo(LIMITE_BRINDE) > 0;
    }
}
