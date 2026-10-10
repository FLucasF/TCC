package com.loja.estrategia;

import com.loja.dto.ItemRequest;
import com.loja.enums.Cupom;
import java.util.List;

public class CupomFactory {
    public static EstrategiaCupom criar(Cupom tipo, List<ItemRequest> itens) {
        if (tipo == null) {
            return null;
        }
        return switch (tipo) {
            case BEMVINDO10 -> new CupomBemvindo10();
            case MENOS50 -> new CupomMenos50();
            case FRETEGRATIS -> new CupomFreteGratis();
            case LEVE3PAGUE2 -> new CupomLeve3Pague2(itens);
        };
    }
}
