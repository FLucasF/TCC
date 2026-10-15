package loja.checkout.clube;

import java.math.BigDecimal;
import loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

@Component
class Ouro implements NivelClube {
    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public boolean isentoDeFrete() {
        return true;
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(new BigDecimal("0.05")));
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(LIMITE_BRINDE) > 0;
    }
}
