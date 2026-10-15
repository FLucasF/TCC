package com.loja.checkout.servico;

import com.loja.checkout.dominio.cupom.*;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class CupomFactory {

    private final Map<String, Cupom> cupons = new HashMap<>();

    public CupomFactory() {
        cupons.put("BEMVINDO10", new BemVindo10());
        cupons.put("MENOS50", new Menos50());
        cupons.put("FRETEGRATIS", new FreteGratis());
        cupons.put("LEVE3PAGUE2", new Leve3Pague2());
    }

    public Cupom obter(String codigo) {
        return cupons.get(codigo);
    }
}
