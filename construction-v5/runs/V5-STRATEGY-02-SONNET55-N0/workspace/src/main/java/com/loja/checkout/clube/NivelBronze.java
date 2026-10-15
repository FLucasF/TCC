package com.loja.checkout.clube;

import org.springframework.stereotype.Component;

@Component
public class NivelBronze implements NivelClube {
    @Override
    public String codigo() {
        return "BRONZE";
    }
}
