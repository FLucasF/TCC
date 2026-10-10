package com.loja.checkout.catalogo;

import com.loja.checkout.dominio.ErroPedido;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Escolhe a opcao pelo codigo que o site enviou. Entrar uma opcao nova e
 * so acrescentar a implementacao: nada aqui muda, e nao existe cadeia de
 * condicoes decidindo qual caso usar.
 */
public final class Catalogo<T extends Identificado> {

    private final Map<String, T> porCodigo;
    private final ErroPedido erroQuandoDesconhecido;

    public Catalogo(Collection<T> opcoes, ErroPedido erroQuandoDesconhecido) {
        this.porCodigo = opcoes.stream().collect(Collectors.toMap(
                Identificado::codigo, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        this.erroQuandoDesconhecido = erroQuandoDesconhecido;
    }

    /** Devolve a opcao do codigo, ou recusa o pedido quando o codigo nao existe. */
    public T exigir(String codigo) {
        T opcao = codigo == null ? null : porCodigo.get(codigo);
        if (opcao == null) {
            throw erroQuandoDesconhecido.erro();
        }
        return opcao;
    }
}
