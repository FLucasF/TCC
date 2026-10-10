package com.loja.checkout.dominio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Indice por codigo das opcoes disponiveis de um tipo (entregas, cupons, clube, pagamentos).
 * Cadastrar uma opcao nova e so criar o bean correspondente: ele entra aqui automaticamente.
 */
public class Catalogo<T extends Codificavel> {

    private final Map<String, T> porCodigo;

    public Catalogo(List<T> opcoes) {
        Map<String, T> mapa = new LinkedHashMap<>();
        for (T opcao : opcoes) {
            T anterior = mapa.put(opcao.codigo(), opcao);
            if (anterior != null) {
                throw new IllegalStateException("Codigo duplicado no catalogo: " + opcao.codigo());
            }
        }
        this.porCodigo = Map.copyOf(mapa);
    }

    /** Busca pelo codigo exato (maiusculas). Vazio quando o codigo nao existe ou nao foi informado. */
    public Optional<T> buscar(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(porCodigo.get(codigo));
    }

    public List<String> codigos() {
        return List.copyOf(porCodigo.keySet());
    }
}
