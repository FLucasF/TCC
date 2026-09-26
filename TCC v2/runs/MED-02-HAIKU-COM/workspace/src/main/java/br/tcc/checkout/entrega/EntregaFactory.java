package br.tcc.checkout.entrega;

import java.util.HashMap;
import java.util.Map;

public class EntregaFactory {
    private static final Map<String, Entrega> ENTREGAS = new HashMap<>();

    static {
        ENTREGAS.put("ECONOMICA", new EconomicaEntrega());
        ENTREGAS.put("EXPRESSA", new ExpressaEntrega());
        ENTREGAS.put("RETIRADA_LOJA", new RetiradaLojaEntrega());
        ENTREGAS.put("MOTOBOY", new MotoplayEntrega());
    }

    public static Entrega criar(String codigo) {
        return ENTREGAS.get(codigo);
    }

    public static boolean existe(String codigo) {
        return ENTREGAS.containsKey(codigo);
    }
}
