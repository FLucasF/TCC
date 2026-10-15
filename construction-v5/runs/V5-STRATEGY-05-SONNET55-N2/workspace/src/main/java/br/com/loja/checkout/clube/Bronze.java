package br.com.loja.checkout.clube;

import org.springframework.stereotype.Component;

@Component
public class Bronze implements NivelClube {
    public String codigo() { return "BRONZE"; }
}
