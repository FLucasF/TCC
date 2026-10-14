package loja.pedido;

import loja.dominio.Pedido;

/**
 * Efeito colateral de um evento do pedido. Para acrescentar (SMS, cashback...), basta um novo @Component.
 * Rodam na mesma transação, na ordem de @Order; uma NegocioException desfaz o pagamento.
 */
public interface AcaoPedido {

    default void aoPagar(Pedido pedido) {
    }

    /** @param estavaPago o pagamento já havia sido confirmado antes do cancelamento */
    default void aoCancelar(Pedido pedido, boolean estavaPago) {
    }
}
