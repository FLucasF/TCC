package com.loja.checkout.aplicacao.regras;

import com.loja.checkout.aplicacao.Rascunho;
import com.loja.checkout.dominio.CodigoErro;
import java.util.Optional;

/**
 * Uma conferencia que o pedido precisa passar. As regras sao conferidas na
 * ordem em que estao cadastradas e vale o primeiro problema encontrado, por
 * isso cada regra pode contar que as anteriores passaram.
 */
public interface Regra {

    Optional<CodigoErro> conferir(Rascunho rascunho);
}
