package br.com.loja.checkout.dominio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Escolhe um caso pelo código que o site envia. É o único lugar que conhece
 * todos os casos de uma dimensão; cada caso cuida do seu comportamento.
 */
public final class Catalogo<T> {

    private final Map<String, T> porCodigo;
    private final MotivoRecusa recusaQuandoNaoExiste;

    private Catalogo(Map<String, T> porCodigo, MotivoRecusa recusaQuandoNaoExiste) {
        this.porCodigo = porCodigo;
        this.recusaQuandoNaoExiste = recusaQuandoNaoExiste;
    }

    public static <T> Catalogo<T> de(MotivoRecusa recusaQuandoNaoExiste,
                                     Function<T, String> codigo,
                                     List<T> casos) {
        Map<String, T> porCodigo = new LinkedHashMap<>();
        casos.forEach(caso -> porCodigo.put(codigo.apply(caso), caso));
        return new Catalogo<>(Map.copyOf(porCodigo), recusaQuandoNaoExiste);
    }

    public T obrigatorio(String codigo) {
        T caso = codigo == null ? null : porCodigo.get(codigo);
        if (caso == null) {
            throw new PedidoRecusadoException(recusaQuandoNaoExiste);
        }
        return caso;
    }
}
