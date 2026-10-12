package br.com.loja.checkout.clube;

import br.com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Ouro implements NivelClube {
    private static final BigDecimal LIMITE_BRINDE = Dinheiro.reais("500.00");

    public String codigo() { return "OURO"; }

    public BigDecimal freteCobrado(BigDecimal frete) {
        return Dinheiro.ZERO;
    }

    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, "0.05");
    }

    public boolean brinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(LIMITE_BRINDE) > 0;
    }
}
