package com.loja.checkout.aplicacao.regras;

import com.loja.checkout.aplicacao.Rascunho;
import com.loja.checkout.dominio.CodigoErro;
import java.util.Optional;

/** A regiao do cliente precisa existir. */
public class RegiaoConhecida implements Regra {

    @Override
    public Optional<CodigoErro> conferir(Rascunho rascunho) {
        return rascunho.regiao().isPresent()
                ? Optional.empty()
                : Optional.of(CodigoErro.REGIAO_INVALIDA);
    }
}
