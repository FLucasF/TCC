package com.loja.checkout.coupon;

public class ResolvedorCupom {
    public static Cupom resolver(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }

        return switch (codigo.toUpperCase()) {
            case "BEMVINDO10" -> new CupomBemvindo10();
            case "MENOS50" -> new CupomMenos50();
            case "FRETEGRATIS" -> new CupomFreteGratis();
            case "LEVE3PAGUE2" -> new CupomLeve3Pague2();
            default -> null;
        };
    }
}
