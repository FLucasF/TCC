package hotel.messages;

/** Sends a message through a channel. Implementations may apply rules before sending. */
public interface MessageSender {

    Message send(String to, String subject, String body);
}
