package br.com.loja.checkout.dominio;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Guarda as opcoes disponiveis de um tipo e resolve o codigo recebido do site.
 * Cadastrar uma opcao nova e so criar a classe dela: o catalogo se monta sozinho.
 */
public final class Catalogo<T extends Codificado> {

    private final Map<String, T> porCodigo = new LinkedHashMap<>();

    public Catalogo(Collection<? extends T> opcoes) {
        opcoes.forEach(opcao -> porCodigo.put(opcao.codigo(), opcao));
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
