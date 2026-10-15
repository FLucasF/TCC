package loja.checkout.clube;

import java.math.BigDecimal;
import loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

@Component
public class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal frete(BigDecimal freteDaEntrega) {
        return freteDaEntrega;
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
