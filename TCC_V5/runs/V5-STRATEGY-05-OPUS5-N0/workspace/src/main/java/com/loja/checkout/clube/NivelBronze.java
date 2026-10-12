package com.loja.checkout.clube;

import org.springframework.stereotype.Component;

/** So o cadastro, sem vantagens. */
@Component
public class NivelBronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }
}
