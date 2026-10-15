package loja.checkout.dominio;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class Catalogo<T extends Identificavel> {
    private final Map<String, T> porCodigo = new LinkedHashMap<>();

    public Catalogo(Collection<? extends T> opcoes) {
        opcoes.forEach(o -> porCodigo.put(o.codigo(), o));
    }

    public T exigir(String codigo, CodigoErro erroSeAusente) {
        T opcao = codigo == null ? null : porCodigo.get(codigo);
        if (opcao == null) {
            throw new PedidoRecusadoException(erroSeAusente);
        }
        return opcao;
    }
}
