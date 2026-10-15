package br.loja.checkout.cupom;

import br.loja.checkout.Identificavel;
import br.loja.checkout.Pedido;
import java.math.BigDecimal;

public interface Cupom extends Identificavel {
    boolean aplicavel(Pedido pedido, BigDecimal frete);

    BigDecimal desconto(Pedido pedido, BigDecimal frete);
}
