package com.loja.checkout.domain.strategy;

public class CupomAplicadorFactory {
    public static CupomAplicador criar(String codigoCupom) {
        if (codigoCupom == null) {
            return null;
        }

        return switch (codigoCupom) {
            case "BEMVINDO10" -> new CupomBemVindo10();
            case "MENOS50" -> new CupomMenos50();
            case "FRETEGRATIS" -> new CupomFreteGratis();
            case "LEVE3PAGUE2" -> new CupomLeve3Pague2();
            default -> null;
        };
    }
}
