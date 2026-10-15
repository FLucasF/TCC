package br.tcc.checkout.dominio.cupom;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RepositorioCupons {
    private static final Map<String, Cupom> CUPONS = new HashMap<>();

    static {
        CUPONS.put("BEMVINDO10", new CupomBemVindo10());
        CUPONS.put("MENOS50", new CupomMenos50());
        CUPONS.put("FRETEGRATIS", new CupomFreteGratis());
        CUPONS.put("LEVE3PAGUE2", new CupomLeve3Pague2());
    }

    public Optional<Cupom> obter(String codigo) {
        return Optional.ofNullable(CUPONS.get(codigo));
    }

    public static RepositorioCupons criar() {
        return new RepositorioCupons();
    }
}
