package hotel.messages;

/** Sends a message to a guest. The rules around sending are wrappers of this interface, put together in {@link MessageSenders}. */
public interface MessageSender {

    Message send(String to, String subject, String body);
}
