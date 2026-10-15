package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Carrinho;
import java.math.BigDecimal;

public interface Cupom {

    String codigo();

    boolean aplicavel(Carrinho carrinho);

    /** Desconto em reais; {@code frete} é o frete que aparece no resumo. */
    BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
