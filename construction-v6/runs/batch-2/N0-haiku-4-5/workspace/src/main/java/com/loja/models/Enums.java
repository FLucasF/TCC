package com.loja.models;

public class Enums {
    public enum ClubTier {
        BRONZE, SILVER, GOLD
    }

    public enum Region {
        SOUTHEAST, SOUTH, MIDWEST, NORTH, NORTHEAST
    }

    public enum ShippingMethod {
        ECONOMY, EXPRESS, STORE_PICKUP, COURIER
    }

    public enum PaymentMethod {
        PIX, CREDIT_CARD, BANK_SLIP
    }

    public enum OrderStatus {
        AWAITING_PAYMENT, PAID, SHIPPED, DELIVERED, CANCELLED
    }

    public enum NotificationType {
        PAYMENT_APPROVED, ORDER_SHIPPED, ORDER_CANCELLED
    }

    public enum NotificationStatus {
        NEW, READ
    }
}
