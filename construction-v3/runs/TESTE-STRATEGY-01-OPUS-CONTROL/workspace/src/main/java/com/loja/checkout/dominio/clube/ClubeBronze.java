package com.loja.checkout.dominio.clube;

import org.springframework.stereotype.Component;

/** Somente o cadastro: nao ganha vantagens. */
@Component
public class ClubeBronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }
}
