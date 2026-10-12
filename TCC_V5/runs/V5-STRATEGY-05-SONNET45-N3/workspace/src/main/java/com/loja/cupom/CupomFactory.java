package com.loja.cupom;

public class CupomFactory {

    public static Cupom criar(String codigo) {
        return switch (codigo) {
            case "BEMVINDO10" -> new BemVindo10();
            case "MENOS50" -> new Menos50();
            case "FRETEGRATIS" -> new FreteGratis();
            case "LEVE3PAGUE2" -> new Leve3Pague2();
            default -> null;
        };
    }
}
