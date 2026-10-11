package br.com.loja.checkout.clube;

import org.springframework.stereotype.Component;

@Component
public class ClubeBronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }
}
