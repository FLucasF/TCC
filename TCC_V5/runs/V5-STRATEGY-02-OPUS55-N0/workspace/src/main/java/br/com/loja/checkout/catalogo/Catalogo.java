package br.com.loja.checkout.catalogo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Conjunto de opções disponíveis, localizadas pelo código exato.
 * Para criar uma opção nova basta registrar mais um componente Spring do tipo correspondente.
 */
public final class Catalogo<T extends Codificado> {

    private final Map<String, T> porCodigo = new HashMap<>();

    public Catalogo(List<T> opcoes) {
        for (T opcao : opcoes) {
            if (porCodigo.putIfAbsent(opcao.codigo(), opcao) != null) {
                throw new IllegalStateException("Código duplicado: " + opcao.codigo());
            }
        }
    }

    public Optional<T> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
