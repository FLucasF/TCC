package com.loja.checkout.dominio.clube;

import org.springframework.stereotype.Component;

/** So o cadastro: nao ganha nada. */
@Component
public class NivelBronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }
}
