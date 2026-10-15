package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Forma de pagamento. Cada forma tem suas parcelas permitidas, sua condicao de
 * disponibilidade e seu jeito de ajustar o total (desconto, tarifa ou juros).
 * Cada forma mora numa classe propria.
 *
 * <p>A assinatura de {@link #calcular} recebe o total e o numero de parcelas
 * para atender o caso mais exigente (cartao com juros); as formas a vista
 * simplesmente ignoram as parcelas.
 */
public interface FormaPagamento {

    String codigo();

    /** Se o numero de parcelas e permitido nesta forma. */
    boolean parcelasValidas(int parcelas);

    /** Se esta forma atende um pedido com esse total. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
