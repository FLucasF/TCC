package com.loja.checkout.clube;

import org.springframework.stereotype.Component;

/** Só o cadastro: não ganha nada. */
@Component
class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }
}
