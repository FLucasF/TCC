package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/**
 * Um cupom de desconto. Cada cupom tem sua própria condição de uso e seu
 * próprio jeito de calcular o desconto.
 */
public interface Cupom {

    String codigo();

    boolean aplicavel(Pedido pedido);

    /**
     * @param frete o frete já com a eventual isenção do clube aplicada, pois
     *              é esse valor que aparece no resumo.
     */
    BigDecimal desconto(Pedido pedido, BigDecimal frete);
}
