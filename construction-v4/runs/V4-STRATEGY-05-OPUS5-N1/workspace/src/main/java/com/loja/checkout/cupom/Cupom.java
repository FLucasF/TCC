package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Identificavel;
import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/** Uma promocao do marketing. Uma implementacao por cupom. */
public interface Cupom extends Identificavel {

    /** Se o pedido cumpre a condicao do cupom. */
    boolean aplicavel(Pedido pedido);

    /** O desconto do cupom. Recebe o frete porque ha cupom que desconta o frete. */
    BigDecimal desconto(Pedido pedido, BigDecimal frete);
}
