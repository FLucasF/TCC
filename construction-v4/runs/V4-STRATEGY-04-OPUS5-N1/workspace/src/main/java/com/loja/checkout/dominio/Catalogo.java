package com.loja.checkout.dominio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Guarda as opcoes de uma familia (entrega, cupom, pagamento) e acha a opcao
 * pelo codigo que o site enviou. Escolher a opcao nunca depende de condicoes:
 * quem nao esta no catalogo nao existe.
 */
public final class Catalogo<T> {

    private final Map<String, T> porCodigo;
    private final CodigoErro erroQuandoNaoExiste;

    private Catalogo(Map<String, T> porCodigo, CodigoErro erroQuandoNaoExiste) {
        this.porCodigo = porCodigo;
        this.erroQuandoNaoExiste = erroQuandoNaoExiste;
    }

    public static <T> Catalogo<T> de(CodigoErro erroQuandoNaoExiste, Function<T, String> codigo, List<T> opcoes) {
        Map<String, T> porCodigo = new LinkedHashMap<>();
        opcoes.forEach(opcao -> porCodigo.put(codigo.apply(opcao), opcao));
        return new Catalogo<>(Map.copyOf(porCodigo), erroQuandoNaoExiste);
    }

    public T buscar(String codigo) {
        T opcao = codigo == null ? null : porCodigo.get(codigo);
        if (opcao == null) {
            throw new PedidoRecusadoException(erroQuandoNaoExiste);
        }
        return opcao;
    }
}
