package loja.checkout.clube;

import java.math.BigDecimal;
import loja.checkout.Dinheiro;
import org.springframework.stereotype.Component;

@Component
public class Ouro implements NivelClube {
    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal frete(BigDecimal freteCalculado) {
        return Dinheiro.arredondar(BigDecimal.ZERO);
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, "0.05");
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(LIMITE_BRINDE) > 0;
    }
}
