package com.loja.checkout.strategy;

public class CupomFactory {

    public static Cupom criar(String codigo) {
        return switch (codigo) {
            case "BEMVINDO10" -> new CupomBemvindo10();
            case "MENOS50" -> new CupomMenos50();
            case "FRETEGRATIS" -> new CupomFreteGratis();
            case "LEVE3PAGUE2" -> new CupomLeve3Pague2();
            default -> null;
        };
    }

}
