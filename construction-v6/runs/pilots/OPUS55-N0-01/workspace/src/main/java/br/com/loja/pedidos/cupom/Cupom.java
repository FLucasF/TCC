package br.com.loja.pedidos.cupom;

import java.math.BigDecimal;

import br.com.loja.pedidos.comum.ErroNegocio;

/** Uma promoção. Para criar um cupom novo, basta criar um novo componente que implemente esta interface. */
public interface Cupom {

    /** O código, em maiúsculas, que o cliente digita. */
    String codigo();

    /** Calcula o desconto; recusa com {@link #naoAplicavel()} quando o pedido não cumpre a condição. */
    BigDecimal desconto(ContextoCupom contexto);

    static ErroNegocio naoAplicavel() {
        return ErroNegocio.invalido("CUPOM_NAO_APLICAVEL");
    }
}
