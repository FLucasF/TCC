package com.loja.checkout.cupom;

import com.loja.checkout.ErroPedido;

import java.util.Map;
import java.util.Optional;

public class RegistroCupons {
    private static final Map<String, Cupom> CUPONS = Map.of(
            "BEMVINDO10", new Bemvindo10(),
            "MENOS50", new Menos50(),
            "FRETEGRATIS", new FreteGratis(),
            "LEVE3PAGUE2", new Leve3Pague2()
    );

    public Optional<Cupom> obter(String nome) {
        if (nome == null) return Optional.empty();
        Cupom c = CUPONS.get(nome);
        if (c == null) throw new ErroPedido("CUPOM_INVALIDO");
        return Optional.of(c);
    }
}
