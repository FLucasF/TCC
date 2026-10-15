package loja.checkout.cupom;

import java.math.BigDecimal;
import loja.checkout.dominio.Carrinho;
import loja.checkout.dominio.Identificavel;

public interface Cupom extends Identificavel {

    /** Desconto do cupom; lança CUPOM_NAO_APLICAVEL se o pedido não cumpre a condição. */
    BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
