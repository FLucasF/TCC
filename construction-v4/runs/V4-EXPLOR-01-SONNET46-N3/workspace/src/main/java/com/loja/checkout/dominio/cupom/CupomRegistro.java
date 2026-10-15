package com.loja.checkout.dominio.cupom;

import com.loja.checkout.infra.CheckoutException;

import java.util.Map;

public class CupomRegistro {

    private static final Map<String, Cupom> REGISTRO = Map.of(
            "BEMVINDO10", new CupomBemVindo10(),
            "MENOS50", new CupomMenos50(),
            "FRETEGRATIS", new CupomFreteGratis(),
            "LEVE3PAGUE2", new CupomLeve3Pague2()
    );

    public static Cupom buscar(String codigo) {
        if (codigo == null || !REGISTRO.containsKey(codigo)) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
        return REGISTRO.get(codigo);
    }
}
