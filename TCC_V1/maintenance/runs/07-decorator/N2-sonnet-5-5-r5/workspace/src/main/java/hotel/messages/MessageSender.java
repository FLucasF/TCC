package hotel.messages;

/** Something that sends a message to a guest. The rules about sending are wrappers around the real sender. */
public interface MessageSender {

    Message send(String to, String subject, String body);
}
