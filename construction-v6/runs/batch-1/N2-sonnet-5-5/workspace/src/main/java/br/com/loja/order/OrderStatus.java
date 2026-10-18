package br.com.loja.order;

public enum OrderStatus {
    AWAITING_PAYMENT, PAID, SHIPPED, DELIVERED, CANCELLED;

    public boolean canMoveTo(OrderStatus target) {
        return switch (this) {
            case AWAITING_PAYMENT -> target == PAID || target == CANCELLED;
            case PAID -> target == SHIPPED || target == CANCELLED;
            case SHIPPED -> target == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
