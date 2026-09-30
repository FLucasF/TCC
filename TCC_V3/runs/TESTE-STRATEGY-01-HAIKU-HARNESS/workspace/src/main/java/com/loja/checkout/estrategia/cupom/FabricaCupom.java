package com.loja.checkout.estrategia.cupom;

import java.math.BigDecimal;
import java.util.List;

public class FabricaCupom {
    public static EstrategiaCupom criar(String codigoCupom, List<BigDecimal> precos) {
        if (codigoCupom == null || codigoCupom.isBlank()) {
            return null;
        }
        return switch (codigoCupom) {
            case "BEMVINDO10" -> new CupomBemVindo10();
            case "MENOS50" -> new CupomMenos50();
            case "FRETEGRATIS" -> new CupomFreteGratis();
            case "LEVE3PAGUE2" -> new CupomLeve3Pague2(precos);
            default -> throw new IllegalArgumentException("CUPOM_INVALIDO");
        };
    }
}
