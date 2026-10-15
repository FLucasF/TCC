package com.loja.checkout.dominio;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Opcoes disponiveis de uma familia (entrega, cupom, clube, pagamento), achadas
 * pelo codigo que o site envia. Quando o codigo nao existe ou nao veio, o pedido
 * e recusado com o erro daquela familia.
 */
public final class Catalogo<T> {

    private final Map<String, T> porCodigo;
    private final Erro erroQuandoNaoExiste;

    private Catalogo(Map<String, T> porCodigo, Erro erroQuandoNaoExiste) {
        this.porCodigo = porCodigo;
        this.erroQuandoNaoExiste = erroQuandoNaoExiste;
    }

    public static <T> Catalogo<T> de(Erro erroQuandoNaoExiste, Map<String, T> opcoes) {
        return new Catalogo<>(Map.copyOf(opcoes), erroQuandoNaoExiste);
    }

    public static <E extends Enum<E>> Catalogo<E> de(Erro erroQuandoNaoExiste, E[] opcoes) {
        return new Catalogo<>(
                Stream.of(opcoes).collect(Collectors.toMap(Enum::name, Function.identity())),
                erroQuandoNaoExiste);
    }

    public T exigir(String codigo) {
        T opcao = codigo == null ? null : porCodigo.get(codigo);
        if (opcao == null) {
            throw new PedidoRecusado(erroQuandoNaoExiste);
        }
        return opcao;
    }
}
