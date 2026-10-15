package com.loja.checkout.factory;

import com.loja.checkout.domain.Cupom;
import com.loja.checkout.domain.cupom.*;
import java.util.Map;

public class CupomFactory {

    private static final Map<String, Cupom> CUPONS = Map.of(
        "BEMVINDO10", new BemVindo10(),
        "MENOS50", new Menos50(),
        "FRETEGRATIS", new FreteGratis(),
        "LEVE3PAGUE2", new Leve3Pague2()
    );

    public static Cupom obter(String codigo) {
        return CUPONS.get(codigo);
    }

    public static boolean existe(String codigo) {
        return CUPONS.containsKey(codigo);
    }
}
