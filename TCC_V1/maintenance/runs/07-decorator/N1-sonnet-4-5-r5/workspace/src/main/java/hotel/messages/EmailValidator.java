package hotel.messages;

/** Bloqueia mensagens para endereços de e-mail inválidos. */
public final class EmailValidator implements MessageProcessor {

    @Override
    public void process(OutgoingMessage message) {
        if (!isValidEmail(message.to())) {
            message.block();
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return false;
        }
        if (atIndex != email.lastIndexOf('@')) {
            return false;
        }
        String domain = email.substring(atIndex + 1);
        if (domain.isEmpty()) {
            return false;
        }
        return domain.contains(".");
    }
}
