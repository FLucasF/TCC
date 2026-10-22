package hotel.messages;

/** Something that sends an e-mail. The rules of the inn are senders that wrap another sender. */
public interface MessageSender {

    Message send(String to, String subject, String body);
}
