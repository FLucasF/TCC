package com.loja.service.estrategia;

import com.loja.domain.Item;
import java.util.List;

public class FabricaCupom {
    public static CalculoCupom criar(String codigo, List<Item> itens) {
        if (codigo == null || codigo.isEmpty()) {
            return new CupomSemDesconto();
        }

        return switch (codigo) {
            case "BEMVINDO10" -> new CupomBemvindo10();
            case "MENOS50" -> new CupomMenos50();
            case "FRETEGRATIS" -> new CupomFreteGratis();
            case "LEVE3PAGUE2" -> new CupomLeve3Pague2(itens);
            default -> null;
        };
    }
}
