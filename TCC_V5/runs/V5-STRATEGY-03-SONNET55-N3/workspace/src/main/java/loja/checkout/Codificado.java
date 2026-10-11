package loja.checkout;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Algo que se escolhe pelo código recebido do site. */
public interface Codificado {
    String codigo();

    static <T extends Codificado> Map<String, T> indexar(List<T> opcoes) {
        return opcoes.stream().collect(Collectors.toMap(Codificado::codigo, Function.identity()));
    }
}
