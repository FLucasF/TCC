package hotel.messages;

import java.util.List;

/** The result of processing a message through handlers. */
public record ProcessedMessage(List<MessageToStore> messages) {

    public static ProcessedMessage sent(String to, String subject, String body) {
        return new ProcessedMessage(List.of(new MessageToStore(to, subject, body, MessageStatus.SENT)));
    }

    public static ProcessedMessage blocked(String to) {
        return new ProcessedMessage(List.of(new MessageToStore(to, "", "", MessageStatus.BLOCKED)));
    }

    public static ProcessedMessage multiple(List<MessageToStore> messages) {
        return new ProcessedMessage(messages);
    }

    public record MessageToStore(String to, String subject, String body, MessageStatus status) {}
}
