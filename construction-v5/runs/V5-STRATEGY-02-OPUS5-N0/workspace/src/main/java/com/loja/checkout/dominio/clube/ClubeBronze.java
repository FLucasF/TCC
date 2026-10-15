package com.loja.checkout.dominio.clube;

import org.springframework.stereotype.Component;

/** Bronze: so o cadastro, nao ganha nada. */
@Component
public class ClubeBronze extends ClubePorPercentual {

    public ClubeBronze() {
        super("0");
    }

    @Override
    public String codigo() {
        return "BRONZE";
    }
}
