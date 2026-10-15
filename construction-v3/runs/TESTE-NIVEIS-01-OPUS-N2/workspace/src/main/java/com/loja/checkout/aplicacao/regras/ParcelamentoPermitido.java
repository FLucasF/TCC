package com.loja.checkout.aplicacao.regras;

import com.loja.checkout.aplicacao.Rascunho;
import com.loja.checkout.dominio.CodigoErro;
import java.util.Optional;

/** O numero de parcelas precisa ser aceito pela forma de pagamento. */
public class ParcelamentoPermitido implements Regra {

    @Override
    public Optional<CodigoErro> conferir(Rascunho rascunho) {
        return rascunho.formaPagamento().orElseThrow().aceitaParcelas(rascunho.parcelas())
                ? Optional.empty()
                : Optional.of(CodigoErro.PARCELAMENTO_INVALIDO);
    }
}
