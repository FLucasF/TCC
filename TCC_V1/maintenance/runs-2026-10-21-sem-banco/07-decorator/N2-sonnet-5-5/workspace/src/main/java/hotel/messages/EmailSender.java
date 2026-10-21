package hotel.messages;

/** Sends e-mails to guests. Every message handled is kept in the list of sent messages. */
public interface EmailSender {

    Message send(String to, String subject, String body);
}
