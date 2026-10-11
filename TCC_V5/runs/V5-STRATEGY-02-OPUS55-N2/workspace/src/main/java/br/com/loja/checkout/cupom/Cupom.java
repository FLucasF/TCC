package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Codificado;

import java.math.BigDecimal;

/** Cupom de desconto. Cada um decide se vale para o pedido e quanto desconta. */
public interface Cupom extends Codificado {

    boolean aplicavel(Carrinho carrinho);

    /**
     * @param frete o frete que o cliente pagaria, já considerando as vantagens do clube
     */
    BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
