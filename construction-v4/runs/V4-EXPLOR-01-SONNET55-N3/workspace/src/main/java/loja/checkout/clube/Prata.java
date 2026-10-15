package loja.checkout.clube;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import loja.checkout.comum.Compra;
import loja.checkout.comum.Dinheiro;

@Component
class Prata implements NivelClube {
    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal credito(Compra compra) {
        return Dinheiro.percentual(compra.subtotal(), new BigDecimal("0.02"));
    }
}
