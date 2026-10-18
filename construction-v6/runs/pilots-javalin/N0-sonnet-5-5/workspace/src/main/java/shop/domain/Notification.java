package shop.domain;

public record Notification(String id, String cpf, NotificationType type, String orderId, String status) {
}
