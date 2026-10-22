package hotel.messages;

/** What happened to a message: it went out, or a rule held it back so the front desk can call the guest. */
public enum MessageStatus {
    SENT,
    BLOCKED
}
