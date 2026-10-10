package br.com.loja.checkout.pedido;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Encontra a opção correspondente a um código. */
public final class Catalogo<T extends Opcao> {

    private final Map<String, T> porCodigo;

    public Catalogo(Collection<? extends T> opcoes) {
        this.porCodigo = opcoes.stream().collect(Collectors.toMap(Opcao::codigo, Function.identity()));
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
