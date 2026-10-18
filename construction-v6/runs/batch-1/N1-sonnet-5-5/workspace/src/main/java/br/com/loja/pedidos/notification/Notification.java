package br.com.loja.pedidos.notification;

public record Notification(String id, NotificationType type, String orderId, String status) {
}
