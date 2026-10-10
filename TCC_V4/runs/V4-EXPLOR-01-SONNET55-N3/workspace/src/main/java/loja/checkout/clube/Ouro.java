package loja.checkout.clube;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import loja.checkout.comum.Compra;
import loja.checkout.comum.Dinheiro;

@Component
class Ouro implements NivelClube {
    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal credito(Compra compra) {
        return Dinheiro.percentual(compra.subtotal(), new BigDecimal("0.05"));
    }

    @Override
    public BigDecimal frete(BigDecimal frete) {
        return Dinheiro.ZERO;
    }

    @Override
    public boolean brinde(Compra compra) {
        return compra.subtotal().compareTo(LIMITE_BRINDE) > 0;
    }
}
