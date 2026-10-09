package com.loja.checkout.aplicacao.regras;

import com.loja.checkout.aplicacao.Rascunho;
import com.loja.checkout.dominio.CodigoErro;
import java.util.Optional;

/** A opcao de entrega precisa atender este pedido. */
public class ModalidadeDisponivel implements Regra {

    @Override
    public Optional<CodigoErro> conferir(Rascunho rascunho) {
        return rascunho.modalidadeEntrega().orElseThrow().atende(rascunho.pedido())
                ? Optional.empty()
                : Optional.of(CodigoErro.MODALIDADE_INDISPONIVEL);
    }
}
