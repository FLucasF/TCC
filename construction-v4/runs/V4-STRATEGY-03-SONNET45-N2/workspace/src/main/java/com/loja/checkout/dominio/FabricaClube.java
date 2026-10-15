package com.loja.checkout.dominio;

import com.loja.checkout.dominio.clube.*;
import java.util.Map;

public class FabricaClube {

    private static final Map<NivelClube, BeneficiosClube> BENEFICIOS = Map.of(
        NivelClube.BRONZE, new ClubeBronze(),
        NivelClube.PRATA, new ClubePrata(),
        NivelClube.OURO, new ClubeOuro()
    );

    public BeneficiosClube obter(NivelClube nivel) {
        return BENEFICIOS.get(nivel);
    }
}
