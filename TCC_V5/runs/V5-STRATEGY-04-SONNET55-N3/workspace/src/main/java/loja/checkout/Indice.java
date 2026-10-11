package loja.checkout;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Localiza um caso pelo código; código ausente ou desconhecido não encontra nada. */
public final class Indice<T> {

    private final Map<String, T> porCodigo;

    public Indice(List<T> casos, Function<T, String> codigo) {
        this.porCodigo = casos.stream().collect(Collectors.toMap(codigo, Function.identity()));
    }

    public T buscar(String codigo, String erro) {
        T caso = codigo == null ? null : porCodigo.get(codigo);
        if (caso == null) {
            throw new ErroCheckout(erro);
        }
        return caso;
    }
}
