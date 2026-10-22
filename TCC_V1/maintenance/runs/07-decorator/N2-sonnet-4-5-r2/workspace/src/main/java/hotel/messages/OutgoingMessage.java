package hotel.messages;

public record OutgoingMessage(String to, String subject, String body) {
}
