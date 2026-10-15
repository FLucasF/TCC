package br.tcc.checkout.cupom;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import br.tcc.checkout.domain.Item;

public class CupomFactory {
    public static Cupom criar(String codigo, List<Item> itens) {
        if (codigo == null) {
            return null;
        }

        return switch (codigo) {
            case "BEMVINDO10" -> new Bemvindo10Cupom();
            case "MENOS50" -> new Menos50Cupom();
            case "FRETEGRATIS" -> new FreteGratisCupom();
            case "LEVE3PAGUE2" -> new Leve3Pague2Cupom(itens);
            default -> null;
        };
    }

    public static boolean existe(String codigo) {
        if (codigo == null) {
            return true;
        }
        return switch (codigo) {
            case "BEMVINDO10", "MENOS50", "FRETEGRATIS", "LEVE3PAGUE2" -> true;
            default -> false;
        };
    }
}
