package loja.checkout.clube;

import java.math.BigDecimal;
import loja.checkout.Dinheiro;
import org.springframework.stereotype.Component;

@Component
public class Prata implements NivelClube {
    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, "0.02");
    }

    @Override
    public BigDecimal freteCobrado(BigDecimal frete) {
        return frete;
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
