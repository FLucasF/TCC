package hotel.messages;

/** Sends a message to a guest. The wrappers around the real sender each add one rule. */
public interface MessageSender {

    Message send(String to, String subject, String body);
}
