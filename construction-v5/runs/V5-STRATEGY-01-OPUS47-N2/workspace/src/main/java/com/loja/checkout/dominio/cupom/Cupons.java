package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.ErroPedido;
import java.util.Map;

public final class Cupons {
    private static final Map<String, Cupom> TODOS = Map.of(
        "BEMVINDO10", new Bemvindo10(),
        "MENOS50", new Menos50(),
        "FRETEGRATIS", new FreteGratis(),
        "LEVE3PAGUE2", new Leve3Pague2()
    );

    private Cupons() {}

    public static Cupom resolver(String codigo) {
        Cupom c = TODOS.get(codigo);
        if (c == null) throw new ErroPedido("CUPOM_INVALIDO");
        return c;
    }
}
