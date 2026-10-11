package loja.checkout.clube;

import java.math.BigDecimal;
import loja.checkout.Dinheiro;
import org.springframework.stereotype.Component;

@Component
public class Bronze implements NivelClube {
    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal frete(BigDecimal freteCalculado) {
        return freteCalculado;
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(BigDecimal.ZERO);
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
