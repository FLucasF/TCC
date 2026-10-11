package loja.checkout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Catalogo<T extends Codificado> {
    private final Map<String, T> porCodigo = new HashMap<>();

    public Catalogo(List<T> opcoes) {
        opcoes.forEach(o -> porCodigo.put(o.codigo(), o));
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }

    public T exigir(String codigo, String erro) {
        return buscar(codigo).orElseThrow(() -> new ErroPedido(erro));
    }
}
