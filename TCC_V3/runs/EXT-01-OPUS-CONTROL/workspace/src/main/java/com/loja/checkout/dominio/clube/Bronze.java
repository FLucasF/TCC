package com.loja.checkout.dominio.clube;

import org.springframework.stereotype.Component;

/** Bronze e so o cadastro: nao ganha nada. */
@Component
public class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }
}
