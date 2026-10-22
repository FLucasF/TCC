package hotel.messages;

import java.util.List;

public final class EmailValidationRule implements MessageRule {

    private final EmailValidator validator;

    public EmailValidationRule(EmailValidator validator) {
        this.validator = validator;
    }

    @Override
    public List<OutgoingMessage> apply(OutgoingMessage message) {
        if (!validator.isValid(message.to())) {
            message.setStatus(MessageStatus.BLOCKED);
        }
        return List.of(message);
    }
}
