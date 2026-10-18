package com.loja.order.domain;

public enum OrderStatus {
  AWAITING_PAYMENT,
  PAID,
  SHIPPED,
  DELIVERED,
  CANCELLED;

  public boolean canTransitionTo(OrderStatus newStatus) {
    if (this == newStatus) return true;

    return switch (this) {
      case AWAITING_PAYMENT -> newStatus == PAID || newStatus == CANCELLED;
      case PAID -> newStatus == SHIPPED || newStatus == CANCELLED;
      case SHIPPED -> newStatus == DELIVERED;
      case DELIVERED, CANCELLED -> false;
    };
  }
}
