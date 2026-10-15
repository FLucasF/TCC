package com.loja.checkout.clube;

import org.springframework.stereotype.Component;

/** Apenas o cadastro: nao ganha vantagens. */
@Component
public class ClubeBronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }
}
