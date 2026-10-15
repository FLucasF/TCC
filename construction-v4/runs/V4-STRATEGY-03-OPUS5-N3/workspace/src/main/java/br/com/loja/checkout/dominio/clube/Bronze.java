package br.com.loja.checkout.dominio.clube;

import org.springframework.stereotype.Component;

import br.com.loja.checkout.dominio.Dinheiro;

/** So o cadastro: nao ganha nada. */
@Component
public class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public Dinheiro credito(Dinheiro subtotalProdutos) {
        return Dinheiro.ZERO;
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
