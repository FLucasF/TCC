package com.loja.checkout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Opções de um tipo indexadas pelo código; encontrar o código errado vira o erro informado. */
public class Catalogo<T extends Identificavel> {
    private final Map<String, T> porCodigo = new HashMap<>();
    private final Erro erroInexistente;

    public Catalogo(List<T> opcoes, Erro erroInexistente) {
        opcoes.forEach(o -> porCodigo.put(o.codigo(), o));
        this.erroInexistente = erroInexistente;
    }

    public T obter(String codigo) {
        T opcao = codigo == null ? null : porCodigo.get(codigo);
        if (opcao == null) {
            throw new ErroNegocioException(erroInexistente);
        }
        return opcao;
    }
}
