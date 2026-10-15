package br.com.loja.checkout.clube;

import org.springframework.stereotype.Component;

/** Somente o cadastro: nao ganha nada. */
@Component
class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }
}
