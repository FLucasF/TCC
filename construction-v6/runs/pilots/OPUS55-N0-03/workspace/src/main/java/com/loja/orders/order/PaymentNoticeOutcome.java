package com.loja.orders.order;

/** Como terminou um aviso de pagamento, para responder igual se o banco repetir o aviso. */
public enum PaymentNoticeOutcome {
    ACCEPTED,
    ORDER_EXPIRED,
    INSUFFICIENT_STOCK
}
