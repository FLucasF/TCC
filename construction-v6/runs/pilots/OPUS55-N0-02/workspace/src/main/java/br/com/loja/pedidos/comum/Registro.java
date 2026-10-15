package br.com.loja.pedidos.comum;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Coleção de opções identificadas por código (modalidades de entrega, cupons, níveis do clube,
 * formas de pagamento). Uma opção nova é só mais um componente do Spring: entra aqui sozinha.
 */
public final class Registro<T> {

    private final Map<String, T> porCodigo = new LinkedHashMap<>();

    public Registro(List<T> opcoes, Function<T, String> codigo) {
        for (T opcao : opcoes) {
            T anterior = porCodigo.put(codigo.apply(opcao), opcao);
            if (anterior != null) {
                throw new IllegalStateException("Código repetido: " + codigo.apply(opcao));
            }
        }
    }

    public Optional<T> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
