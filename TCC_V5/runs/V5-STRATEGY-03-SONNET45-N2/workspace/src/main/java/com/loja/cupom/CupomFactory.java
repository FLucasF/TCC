package com.loja.cupom;

import com.loja.exception.PedidoException;
import java.util.Map;

public class CupomFactory {
    private static final Map<String, Cupom> CUPONS = Map.of(
        "BEMVINDO10", new BemVindo10(),
        "MENOS50", new Menos50(),
        "FRETEGRATIS", new FreteGratis(),
        "LEVE3PAGUE2", new Leve3Pague2()
    );

    public static Cupom criar(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return new SemCupom();
        }

        Cupom cupom = CUPONS.get(codigo);
        if (cupom == null) {
            throw new PedidoException("CUPOM_INVALIDO");
        }

        return cupom;
    }
}
