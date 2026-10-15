package com.loja.checkout.service.clube;

import com.loja.checkout.model.NivelClube;

public class FactoryClube {
    public static EstrategiaClube criar(NivelClube nivel) {
        return switch (nivel) {
            case BRONZE -> new EstrategiaClube.Bronze();
            case PRATA -> new EstrategiaClube.Prata();
            case OURO -> new EstrategiaClube.Ouro();
        };
    }
}
