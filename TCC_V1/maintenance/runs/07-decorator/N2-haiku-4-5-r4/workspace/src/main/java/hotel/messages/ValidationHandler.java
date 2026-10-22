package hotel.messages;

public final class ValidationHandler implements MessageHandler {
    @Override
    public void handle(MessageRequest request) {
        if (!EmailValidator.isValid(request.to())) {
            request.block();
        }
    }
}
