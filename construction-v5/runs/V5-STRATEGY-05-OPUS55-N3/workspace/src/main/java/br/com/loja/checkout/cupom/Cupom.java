package br.com.loja.checkout.cupom;

import java.math.BigDecimal;

import br.com.loja.checkout.Codificado;

public interface Cupom extends Codificado {

    boolean aplicavel(ContextoCupom contexto);

    BigDecimal desconto(ContextoCupom contexto);
}
