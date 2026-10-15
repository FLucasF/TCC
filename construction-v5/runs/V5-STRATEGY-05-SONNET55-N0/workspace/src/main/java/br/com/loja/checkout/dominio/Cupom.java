package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Cupom de desconto. Para criar um novo, basta registrar outro bean desta interface. */
public interface Cupom {

    String codigo();

    boolean aplicavel(Contexto contexto);

    BigDecimal desconto(Contexto contexto);

    /** Dados do pedido que um cupom pode consultar. O frete já considera as vantagens do clube. */
    record Contexto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
    }
}
