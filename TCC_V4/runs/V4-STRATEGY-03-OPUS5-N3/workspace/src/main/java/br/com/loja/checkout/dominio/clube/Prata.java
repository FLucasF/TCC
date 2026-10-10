package br.com.loja.checkout.dominio.clube;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.checkout.dominio.Dinheiro;

/** 2% dos produtos de volta em credito. */
@Component
public class Prata implements NivelClube {

    private static final BigDecimal TAXA_CREDITO = new BigDecimal("0.02");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public Dinheiro credito(Dinheiro subtotalProdutos) {
        return subtotalProdutos.vezes(TAXA_CREDITO);
    }

    @Override
    public Dinheiro frete(Dinheiro freteDaEntrega) {
        return freteDaEntrega;
    }

    @Override
    public boolean temBrinde(Dinheiro subtotalProdutos) {
        return false;
    }
}
