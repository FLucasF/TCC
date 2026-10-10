package com.loja.checkout.dominio;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * As opcoes que existem de um tipo, achadas pelo codigo. Entrar uma opcao nova
 * e' registra-la aqui; nenhum calculo muda.
 */
public final class Catalogo<T extends Identificavel> {

    private final Map<String, T> porCodigo;
    private final ErroCheckout erroQuandoDesconhecido;

    public Catalogo(ErroCheckout erroQuandoDesconhecido, Collection<T> opcoes) {
        this.erroQuandoDesconhecido = erroQuandoDesconhecido;
        this.porCodigo = opcoes.stream().collect(Collectors.toMap(
                Identificavel::codigo, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    /** A opcao daquele codigo, ou recusa o pedido se o codigo nao existe. */
    public T resolver(String codigo) {
        T opcao = codigo == null ? null : porCodigo.get(codigo);
        if (opcao == null) {
            throw new PedidoRecusadoException(erroQuandoDesconhecido);
        }
        return opcao;
    }
}
