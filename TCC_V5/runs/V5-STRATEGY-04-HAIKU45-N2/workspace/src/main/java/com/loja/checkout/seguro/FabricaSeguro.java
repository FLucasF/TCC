package com.loja.checkout.seguro;

import com.loja.checkout.model.Regiao;

public class FabricaSeguro {
    public static CalculadoraSeguro criar(Regiao regiao) {
        if (regiao == null) {
            return null;
        }

        return switch (regiao) {
            case SUDESTE -> new SeguroSudeste();
            case SUL -> new SeguroSul();
            case CENTRO_OESTE -> new SegurocCentroOeste();
            case NORTE -> new SeguroNorte();
            case NORDESTE -> new SeguroNordeste();
        };
    }
}
