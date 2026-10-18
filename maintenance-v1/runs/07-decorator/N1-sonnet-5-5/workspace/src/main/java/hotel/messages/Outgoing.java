package hotel.messages;

/** An e-mail on its way out, before it is recorded. A blocked one is recorded but never delivered. */
record Outgoing(String to, String subject, String body, boolean blocked) {

    Outgoing block() {
        return new Outgoing(to, subject, body, true);
    }
}
