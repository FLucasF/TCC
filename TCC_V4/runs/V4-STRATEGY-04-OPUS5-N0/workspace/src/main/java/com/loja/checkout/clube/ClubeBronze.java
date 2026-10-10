package com.loja.checkout.clube;

import org.springframework.stereotype.Component;

/** So o cadastro: nao ganha nada. */
@Component
public class ClubeBronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }
}
