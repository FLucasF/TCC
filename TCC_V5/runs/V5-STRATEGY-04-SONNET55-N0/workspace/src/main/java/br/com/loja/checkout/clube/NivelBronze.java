package br.com.loja.checkout.clube;

import org.springframework.stereotype.Component;

@Component
class NivelBronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }
}
