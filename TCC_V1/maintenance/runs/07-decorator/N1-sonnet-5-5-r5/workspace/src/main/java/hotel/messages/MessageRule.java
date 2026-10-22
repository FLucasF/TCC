package hotel.messages;

/** One rule of the inn about its e-mails. It receives the draft and gives back the draft that goes on. */
public interface MessageRule {

    Draft apply(Draft draft);
}
