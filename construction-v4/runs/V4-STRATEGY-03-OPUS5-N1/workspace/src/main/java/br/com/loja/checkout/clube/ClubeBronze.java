package br.com.loja.checkout.clube;

import br.com.loja.checkout.calculo.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class ClubeBronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    @Override
    public boolean isentaFrete() {
        return false;
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
