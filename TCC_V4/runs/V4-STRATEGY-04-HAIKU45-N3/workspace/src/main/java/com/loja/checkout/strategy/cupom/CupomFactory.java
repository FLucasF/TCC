package com.loja.checkout.strategy.cupom;

import com.loja.checkout.exception.CheckoutException;

public class CupomFactory {
    public static CupomStrategy criar(String codigoCupom) {
        if (codigoCupom == null) {
            return null;
        }

        return switch (codigoCupom) {
            case "BEMVINDO10" -> new Bemvindo10Strategy();
            case "MENOS50" -> new Menos50Strategy();
            case "FRETEGRATIS" -> new FreteGratisStrategy();
            case "LEVE3PAGUE2" -> new Leve3Pague2Strategy();
            default -> throw new CheckoutException("CUPOM_INVALIDO");
        };
    }
}
