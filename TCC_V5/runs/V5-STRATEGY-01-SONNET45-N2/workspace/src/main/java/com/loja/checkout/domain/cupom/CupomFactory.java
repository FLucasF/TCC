package com.loja.checkout.domain.cupom;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import java.util.Map;

public class CupomFactory {
    private static final Map<String, Cupom> CUPONS = Map.of(
        "BEMVINDO10", new BemVindo10(),
        "MENOS50", new Menos50(),
        "FRETEGRATIS", new FreteGratis(),
        "LEVE3PAGUE2", new Leve3Pague2()
    );

    public static Cupom criar(String codigo) {
        if (!CUPONS.containsKey(codigo)) {
            throw new CheckoutException(CodigoErro.CUPOM_INVALIDO);
        }
        return CUPONS.get(codigo);
    }
}
