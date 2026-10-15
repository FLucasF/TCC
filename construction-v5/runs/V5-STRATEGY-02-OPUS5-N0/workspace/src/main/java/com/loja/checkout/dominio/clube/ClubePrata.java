package com.loja.checkout.dominio.clube;

import org.springframework.stereotype.Component;

/** Prata: 2% dos produtos de volta em credito. */
@Component
public class ClubePrata extends ClubePorPercentual {

    public ClubePrata() {
        super("0.02");
    }

    @Override
    public String codigo() {
        return "PRATA";
    }
}
