package br.com.loja.checkout.cupom;

import br.com.loja.checkout.resumo.Carrinho;
import br.com.loja.checkout.resumo.Codificado;
import java.math.BigDecimal;

/** Cada cupom decide se vale para o pedido e quanto desconta. */
public interface Cupom extends Codificado {

    boolean aplicavel(Carrinho carrinho);

    /** Desconto em centavos; {@code frete} é o frete que aparece no resumo. */
    BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
