package com.loja.checkout.dominio.clube;

import org.springframework.stereotype.Component;

/** Só o cadastro: nao ganha nada. */
@Component
public class ClubeBronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }
}
