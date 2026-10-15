package com.loja.checkout.aplicacao.regras;

import com.loja.checkout.aplicacao.Rascunho;
import com.loja.checkout.dominio.CodigoErro;
import java.util.List;
import java.util.Optional;

/**
 * As conferencias do pedido, na ordem. Devolve o primeiro problema encontrado e
 * nao confere as regras seguintes.
 */
public final class RegrasDoPedido {

    private final List<Regra> regras;

    public RegrasDoPedido(List<Regra> regras) {
        this.regras = List.copyOf(regras);
    }

    public Optional<CodigoErro> primeiroProblema(Rascunho rascunho) {
        return regras.stream()
                .map(regra -> regra.conferir(rascunho))
                .flatMap(Optional::stream)
                .findFirst();
    }
}
