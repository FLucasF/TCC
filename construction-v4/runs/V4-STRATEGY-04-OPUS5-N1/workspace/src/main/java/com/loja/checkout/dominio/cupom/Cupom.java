package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/** Um cupom de desconto: sua condicao e seu jeito de descontar. */
public interface Cupom {

    String codigo();

    /** Se o pedido cumpre a condicao do cupom. */
    boolean aplicavel(Pedido pedido, BigDecimal frete);

    /** Desconto do cupom, arredondado para centavos. */
    BigDecimal desconto(Pedido pedido, BigDecimal frete);
}
