package hotel.messages;

/** A rule that transforms or validates outbound e-mails. */
interface EmailRule {
    void apply(OutboundMessage message);
}
