package com.loja.checkout.clube;

import com.loja.checkout.model.enums.NivelClube;
import java.util.Map;

public class FabricaBeneficioClube {
    private static final Map<NivelClube, BeneficioClube> BENEFICIOS = Map.of(
        NivelClube.BRONZE, new BeneficiosBronze(),
        NivelClube.PRATA, new BeneficiosPrata(),
        NivelClube.OURO, new BeneficiosOuro()
    );

    public static BeneficioClube criar(NivelClube nivel) {
        return BENEFICIOS.get(nivel);
    }
}
