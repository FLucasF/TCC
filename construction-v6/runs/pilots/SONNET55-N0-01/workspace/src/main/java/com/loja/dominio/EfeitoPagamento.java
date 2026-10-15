package com.loja.dominio;

/**
 * Algo que acontece quando o pagamento é confirmado e que se desfaz se o pedido pago for cancelado
 * (ex.: crédito do clube, pontos de cashback). Para criar um novo, basta uma nova classe @Component.
 */
public interface EfeitoPagamento {

    void aplicar(Estado estado, Mudancas mudancas, Pedido pedido);

    void reverter(Estado estado, Mudancas mudancas, Pedido pedido);
}
