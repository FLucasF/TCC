package br.com.loja.pedidos.checkout.cupom;

import java.math.BigDecimal;

/**
 * Um cupom de desconto. Para criar um cupom novo, basta um novo componente do Spring que
 * implemente esta interface. O limite de 30% dos produtos é aplicado por fora, para todos.
 */
public interface Cupom {

    /** O código, em letras maiúsculas, exatamente como o cliente digita. */
    String codigo();

    /**
     * @return o desconto, em reais, antes do limite geral dos cupons
     * @throws br.com.loja.pedidos.comum.ErroNegocio {@code CUPOM_NAO_APLICAVEL} se o pedido não cumpre a condição
     */
    BigDecimal desconto(PedidoParaCupom pedido);
}
