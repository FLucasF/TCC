package br.com.loja.notification;

public record Notification(long id, String type, String orderId, String status) {
}
