package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Codificado;
import com.loja.checkout.dominio.Entrega;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/**
 * Uma promocao. Cada cupom diz se o pedido cumpre a condicao dele e quanto
 * desconta. Para criar uma promocao nova, basta uma classe nova.
 */
public interface Cupom extends Codificado {

    default boolean aplicavel(Pedido pedido, Entrega entrega) {
        return true;
    }

    BigDecimal desconto(Pedido pedido, Entrega entrega);
}
