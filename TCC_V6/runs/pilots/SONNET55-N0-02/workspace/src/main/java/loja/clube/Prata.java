package loja.clube;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import loja.dominio.Dinheiro;

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
}
