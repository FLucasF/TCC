package br.com.loja.checkout.clube;

import org.springframework.stereotype.Component;

/** Só o cadastro, sem vantagens. */
@Component
public class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }
}
