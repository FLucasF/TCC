package com.loja.checkout.strategy.clube;

import com.loja.checkout.model.NivelClube;
import com.loja.checkout.exception.CheckoutException;

public class ClubeMemberFactory {
    public static ClubeMemberStrategy criar(NivelClube nivel) {
        if (nivel == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }

        return switch (nivel) {
            case BRONZE -> new BronzeStrategy();
            case PRATA -> new PrataStrategy();
            case OURO -> new OuroStrategy();
        };
    }
}
