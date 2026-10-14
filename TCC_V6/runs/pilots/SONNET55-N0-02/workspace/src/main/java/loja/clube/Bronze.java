package loja.clube;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import loja.dominio.Dinheiro;

@Component
public class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }
}
