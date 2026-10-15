package com.loja.checkout.dominio;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Escolhe o caso pelo código, por consulta num mapa: entrar um caso novo é
 * entrar uma classe nova, sem mexer em quem escolhe.
 */
public class Catalogo<T extends Identificado> {

    private final Map<String, T> porCodigo = new LinkedHashMap<>();
    private final CodigoErro erroQuandoNaoExiste;

    public Catalogo(Collection<T> casos, CodigoErro erroQuandoNaoExiste) {
        this.erroQuandoNaoExiste = erroQuandoNaoExiste;
        for (T caso : casos) {
            T repetido = porCodigo.put(caso.codigo(), caso);
            if (repetido != null) {
                throw new IllegalStateException(
                        "Dois casos com o código " + caso.codigo() + ": "
                                + repetido.getClass().getSimpleName() + " e "
                                + caso.getClass().getSimpleName());
            }
        }
    }

    public T obrigatorio(String codigo) {
        T caso = codigo == null ? null : porCodigo.get(codigo);
        if (caso == null) {
            throw new PedidoRecusadoException(erroQuandoNaoExiste);
        }
        return caso;
    }
}
