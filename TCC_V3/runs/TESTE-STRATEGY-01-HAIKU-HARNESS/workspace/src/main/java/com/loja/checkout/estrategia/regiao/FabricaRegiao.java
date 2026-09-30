package com.loja.checkout.estrategia.regiao;

import com.loja.checkout.modelo.Regiao;

public class FabricaRegiao {
    public static EstrategiaRegiao criar(String regiaoStr) {
        if (regiaoStr == null || regiaoStr.isBlank()) {
            throw new IllegalArgumentException("REGIAO_INVALIDA");
        }
        try {
            Regiao regiao = Regiao.valueOf(regiaoStr);
            return switch (regiao) {
                case SUDESTE -> new RegioSudeste();
                case SUL -> new RegioSul();
                case CENTRO_OESTE -> new RegioCentroOeste();
                case NORTE -> new RegioNorte();
                case NORDESTE -> new RegioNordeste();
            };
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("REGIAO_INVALIDA");
        }
    }
}
