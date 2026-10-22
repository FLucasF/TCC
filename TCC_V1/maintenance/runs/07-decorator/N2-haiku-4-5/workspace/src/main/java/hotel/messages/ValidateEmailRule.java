package hotel.messages;

public final class ValidateEmailRule implements EmailRule {

    @Override
    public ProcessingResult process(EmailToSend email) {
        if (!EmailValidator.isValid(email.to())) {
            return ProcessingResult.blocked();
        }
        return ProcessingResult.passed(email);
    }
}
