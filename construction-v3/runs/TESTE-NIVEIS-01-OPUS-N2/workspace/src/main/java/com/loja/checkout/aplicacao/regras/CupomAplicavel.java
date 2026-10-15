package com.loja.checkout.aplicacao.regras;

import com.loja.checkout.aplicacao.Rascunho;
import com.loja.checkout.dominio.CodigoErro;
import java.util.Optional;

/** O pedido precisa cumprir a condicao da promocao. */
public class CupomAplicavel implements Regra {

    @Override
    public Optional<CodigoErro> conferir(Rascunho rascunho) {
        boolean valido = rascunho.cupom()
                .map(cupom -> cupom.aplicavel(rascunho.contextoCupom()))
                .orElse(true);
        return valido ? Optional.empty() : Optional.of(CodigoErro.CUPOM_NAO_APLICAVEL);
    }
}
