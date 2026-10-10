package br.com.loja.checkout.dominio;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registro dos casos de um eixo de variacao, indexados pelo codigo. Escolher um
 * caso e buscar aqui; nenhum lugar do sistema decide por cadeia de condicoes.
 */
public final class Registro<T extends Identificavel> {

    private final Map<String, T> porCodigo = new LinkedHashMap<>();

    public Registro(Collection<T> casos) {
        casos.forEach(caso -> {
            T repetido = porCodigo.put(caso.codigo(), caso);
            if (repetido != null) {
                throw new IllegalStateException("Dois casos com o codigo " + caso.codigo());
            }
        });
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
