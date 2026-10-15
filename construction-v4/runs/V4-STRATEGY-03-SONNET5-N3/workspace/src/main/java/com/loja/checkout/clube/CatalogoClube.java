package com.loja.checkout.clube;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CatalogoClube {

    private final Map<String, BeneficioClube> niveis = Map.of(
            "BRONZE", new Bronze(),
            "PRATA", new Prata(),
            "OURO", new Ouro()
    );

    public BeneficioClube buscar(String nivel) {
        return niveis.get(nivel);
    }
}
