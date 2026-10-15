package com.loja.checkout.dominio;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Guarda as opções disponíveis de um tipo e as encontra pelo código que o site envia. */
public final class Catalogo<T extends Identificado> {

    private final Map<String, T> porCodigo = new LinkedHashMap<>();
    private final Erro erroQuandoNaoExiste;

    public Catalogo(Collection<T> opcoes, Erro erroQuandoNaoExiste) {
        this.erroQuandoNaoExiste = erroQuandoNaoExiste;
        opcoes.forEach(opcao -> porCodigo.put(opcao.codigo(), opcao));
    }

    public T buscar(String codigo) {
        T opcao = codigo == null ? null : porCodigo.get(codigo);
        if (opcao == null) {
            throw erroQuandoNaoExiste.recusar();
        }
        return opcao;
    }
}
