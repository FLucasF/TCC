package com.loja.checkout.dominio;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resolve um código vindo do pedido na implementação do caso. Escolher o caso é
 * uma busca no mapa, não uma cadeia de condições; código ausente ou desconhecido
 * vira o erro do eixo.
 */
public final class Catalogo<T> {

    private final Map<String, T> porCodigo;
    private final String erroCodigoInvalido;

    public Catalogo(Map<String, T> porCodigo, String erroCodigoInvalido) {
        this.porCodigo = new LinkedHashMap<>(porCodigo);
        this.erroCodigoInvalido = erroCodigoInvalido;
    }

    public T exigir(String codigo) {
        T encontrado = codigo == null ? null : porCodigo.get(codigo);
        if (encontrado == null) {
            throw new ErroPedido(erroCodigoInvalido);
        }
        return encontrado;
    }
}
