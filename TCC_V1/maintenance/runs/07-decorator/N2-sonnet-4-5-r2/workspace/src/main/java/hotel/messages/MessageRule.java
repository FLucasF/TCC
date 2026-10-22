package hotel.messages;

/** A rule applied to outgoing messages before they are sent. */
interface MessageRule {

    void apply(MessageContext context);
}
