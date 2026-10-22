package hotel.messages;

public sealed interface EmailRule permits ValidateEmailRule, TestModeRule {
    ProcessingResult process(EmailToSend email);
}
