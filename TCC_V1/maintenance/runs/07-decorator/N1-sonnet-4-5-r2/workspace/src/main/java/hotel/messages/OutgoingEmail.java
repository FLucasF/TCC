package hotel.messages;

record OutgoingEmail(String to, String subject, String body, MessageStatus status) {

    static OutgoingEmail sent(String to, String subject, String body) {
        return new OutgoingEmail(to, subject, body, MessageStatus.SENT);
    }

    static OutgoingEmail blocked(String to, String subject, String body) {
        return new OutgoingEmail(to, subject, body, MessageStatus.BLOCKED);
    }
}
