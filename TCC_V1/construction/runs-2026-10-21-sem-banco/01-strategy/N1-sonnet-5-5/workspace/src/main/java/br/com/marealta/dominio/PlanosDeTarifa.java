package br.com.marealta.dominio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Catálogo dos planos de tarifa. Para um plano novo, basta acrescentá-lo à lista. */
public final class PlanosDeTarifa {

    private static final List<PlanoDeTarifa> PLANOS = List.of(
            new PlanoNormal(),
            new PlanoNaoReembolsavel(),
            new PlanoConvenio(),
            new PlanoFimDeSemana(),
            new PlanoEstadiaLonga());

    private static final Map<String, PlanoDeTarifa> POR_CODIGO = indexar();

    private PlanosDeTarifa() {
    }

    private static Map<String, PlanoDeTarifa> indexar() {
        Map<String, PlanoDeTarifa> mapa = new LinkedHashMap<>();
        PLANOS.forEach(plano -> mapa.put(plano.codigo(), plano));
        return mapa;
    }

    public static PlanoDeTarifa porCodigo(String codigo) {
        PlanoDeTarifa plano = POR_CODIGO.get(codigo);
        if (plano == null) {
            throw ApiException.invalida("Plano de tarifa desconhecido: " + codigo);
        }
        return plano;
    }
}
