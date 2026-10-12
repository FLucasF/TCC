package loja.checkout.clube;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import loja.checkout.Dinheiro;

@Component
public class Ouro implements NivelClube {
    private static final BigDecimal CREDITO = new BigDecimal("0.05");
    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, CREDITO);
    }

    @Override
    public boolean isentoDeFrete() {
        return true;
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
    }
}
