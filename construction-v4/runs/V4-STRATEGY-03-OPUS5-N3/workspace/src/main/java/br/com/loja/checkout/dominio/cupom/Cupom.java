package br.com.loja.checkout.dominio.cupom;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Identificavel;

/**
 * Uma promocao. Cada cupom guarda sua condicao e sua conta de desconto; cupom
 * novo do marketing e uma classe nova aqui.
 */
public interface Cupom extends Identificavel {

    /** Se o pedido cumpre a condicao do cupom. */
    boolean aplicavel(ContextoCupom contexto);

    Dinheiro desconto(ContextoCupom contexto);
}
