package com.loja.checkout.aplicacao.regras;

import com.loja.checkout.aplicacao.Rascunho;
import com.loja.checkout.dominio.CodigoErro;
import java.util.Optional;

/** A opcao de entrega precisa existir. */
public class ModalidadeConhecida implements Regra {

    @Override
    public Optional<CodigoErro> conferir(Rascunho rascunho) {
        return rascunho.modalidadeEntrega().isPresent()
                ? Optional.empty()
                : Optional.of(CodigoErro.MODALIDADE_INVALIDA);
    }
}
