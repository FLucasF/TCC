package com.loja.orders.coupon;

public interface PurchaseHistory {

    /** Se o CPF já tem algum pedido pago, enviado ou entregue. */
    boolean hasPurchased(String cpf);
}
