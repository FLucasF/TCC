package com.loja.checkout.aplicacao.regras;

import com.loja.checkout.aplicacao.Rascunho;
import com.loja.checkout.dominio.CodigoErro;
import java.util.Optional;

/** Cupom informado precisa existir; sem cupom informado, nada a conferir. */
public class CupomConhecido implements Regra {

    @Override
    public Optional<CodigoErro> conferir(Rascunho rascunho) {
        boolean valido = !rascunho.informouCupom() || rascunho.cupom().isPresent();
        return valido ? Optional.empty() : Optional.of(CodigoErro.CUPOM_INVALIDO);
    }
}
