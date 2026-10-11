package br.com.loja.checkout.dominio;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Localiza a opção correspondente ao código enviado pelo site. */
public final class Catalogo<T> {

    private final Map<String, T> porCodigo;

    public Catalogo(Collection<T> opcoes, Function<T, String> codigo) {
        this.porCodigo = opcoes.stream().collect(Collectors.toUnmodifiableMap(codigo, Function.identity()));
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }

    public T exigir(String codigo, CodigoErro erroSeAusente) {
        return buscar(codigo).orElseThrow(() -> new PedidoRecusadoException(erroSeAusente));
    }
}
