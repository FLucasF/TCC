package hotel.messages;

/** Sends e-mails to guests. Every e-mail handled is kept in the list of messages, sent or blocked. */
public interface EmailSender {

    Message send(String to, String subject, String body);
}
