package br.com.loja.pedidos.order;

import java.util.Arrays;
import java.util.List;

/** O que cada situação permite fica aqui; uma situação nova (ex.: em separação) entra só neste enum. */
public enum OrderStatus {
    AWAITING_PAYMENT(true, false, false, true, false),
    PAID(false, true, false, true, true),
    SHIPPED(false, false, true, false, true),
    DELIVERED(false, false, false, false, true),
    CANCELLED(false, false, false, false, false);

    private final boolean acceptsPayment;
    private final boolean canShip;
    private final boolean canDeliver;
    private final boolean canCancel;
    private final boolean paymentReceived;

    OrderStatus(boolean acceptsPayment, boolean canShip, boolean canDeliver, boolean canCancel,
                boolean paymentReceived) {
        this.acceptsPayment = acceptsPayment;
        this.canShip = canShip;
        this.canDeliver = canDeliver;
        this.canCancel = canCancel;
        this.paymentReceived = paymentReceived;
    }

    public boolean acceptsPayment() {
        return acceptsPayment;
    }

    public boolean canShip() {
        return canShip;
    }

    public boolean canDeliver() {
        return canDeliver;
    }

    public boolean canCancel() {
        return canCancel;
    }

    /** Verdadeiro quando o pagamento já foi confirmado (e o pedido não foi cancelado). */
    public boolean paymentReceived() {
        return paymentReceived;
    }

    public static List<OrderStatus> withPaymentReceived() {
        return Arrays.stream(values()).filter(OrderStatus::paymentReceived).toList();
    }
}
