package hotel.messages;

public record ProcessedMessage(String to, String subject, String body, MessageStatus status) {
}
