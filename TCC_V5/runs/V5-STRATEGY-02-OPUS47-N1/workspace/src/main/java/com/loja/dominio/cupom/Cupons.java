package com.loja.dominio.cupom;

import com.loja.dominio.ErroCheckout;

import java.util.Map;
import java.util.function.Supplier;

public final class Cupons {
    private static final Map<String, Supplier<Cupom>> REGISTRO = Map.of(
        "BEMVINDO10", Bemvindo10::new,
        "MENOS50", Menos50::new,
        "FRETEGRATIS", FreteGratis::new,
        "LEVE3PAGUE2", Leve3Pague2::new
    );

    private Cupons() {}

    public static Cupom parse(String codigo) {
        var sup = REGISTRO.get(codigo);
        if (sup == null) throw new ErroCheckout("CUPOM_INVALIDO");
        return sup.get();
    }
}
