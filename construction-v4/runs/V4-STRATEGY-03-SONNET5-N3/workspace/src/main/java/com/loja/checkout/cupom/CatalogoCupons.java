package com.loja.checkout.cupom;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CatalogoCupons {

    private final Map<String, Cupom> cupons = Map.of(
            "BEMVINDO10", new Bemvindo10(),
            "MENOS50", new Menos50(),
            "FRETEGRATIS", new FreteGratis(),
            "LEVE3PAGUE2", new Leve3Pague2()
    );

    public Cupom buscar(String codigo) {
        return cupons.get(codigo);
    }
}
