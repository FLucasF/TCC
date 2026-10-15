package loja.checkout.clube;

import java.math.BigDecimal;
import loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

@Component
class Prata implements NivelClube {
    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(new BigDecimal("0.02")));
    }
}
