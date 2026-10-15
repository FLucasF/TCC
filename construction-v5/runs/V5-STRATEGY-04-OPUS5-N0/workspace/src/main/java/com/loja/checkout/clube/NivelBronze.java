package com.loja.checkout.clube;

import org.springframework.stereotype.Component;

/** Só o cadastro: não ganha nada. */
@Component
public class NivelBronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }
}
