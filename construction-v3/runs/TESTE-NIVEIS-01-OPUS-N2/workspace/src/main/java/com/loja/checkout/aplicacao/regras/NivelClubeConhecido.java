package com.loja.checkout.aplicacao.regras;

import com.loja.checkout.aplicacao.Rascunho;
import com.loja.checkout.dominio.CodigoErro;
import java.util.Optional;

/** O nivel do clube precisa existir. */
public class NivelClubeConhecido implements Regra {

    @Override
    public Optional<CodigoErro> conferir(Rascunho rascunho) {
        return rascunho.nivelClube().isPresent()
                ? Optional.empty()
                : Optional.of(CodigoErro.NIVEL_CLUBE_INVALIDO);
    }
}
