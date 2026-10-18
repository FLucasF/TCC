package hotel.messages;

/** One rule of the inn about outgoing e-mails. To add a new rule, implement this and list it in {@link MessageRules}. */
public interface MessageRule {

    void apply(OutgoingEmail email);
}
