package com.loja.checkout.dominio;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registro de casos de um eixo (entrega, cupom, clube, pagamento): a escolha do
 * caso e sempre uma busca pelo codigo, nunca uma sequencia de condicoes.
 */
public final class Registro<T extends Registro.Identificado> {

    /** Todo caso registrado se identifica pelo codigo que o site envia. */
    public interface Identificado {
        String codigo();
    }

    private final Map<String, T> porCodigo;

    @SafeVarargs
    public Registro(T... casos) {
        Map<String, T> mapa = new LinkedHashMap<>();
        for (T caso : casos) {
            mapa.put(caso.codigo(), caso);
        }
        this.porCodigo = Map.copyOf(mapa);
    }

    public Optional<T> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }

}
