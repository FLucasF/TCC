package com.loja.checkout.aplicacao.regras;

import com.loja.checkout.aplicacao.Rascunho;
import com.loja.checkout.dominio.CodigoErro;
import java.util.Optional;

/** A forma de pagamento precisa atender este pedido. */
public class FormaPagamentoDisponivel implements Regra {

    @Override
    public Optional<CodigoErro> conferir(Rascunho rascunho) {
        return rascunho.formaPagamento().orElseThrow().atende(rascunho.totalPedido())
                ? Optional.empty()
                : Optional.of(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }
}
