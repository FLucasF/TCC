package hotel.messages;

public final class TestModeRule implements EmailRule {

    @Override
    public ProcessingResult process(EmailToSend email) {
        String testMode = System.getenv("HOTEL_TEST_MODE");
        if (!"true".equals(testMode)) {
            return ProcessingResult.passed(email);
        }
        String originalTo = email.to();
        email.setTo("testes@pousadamarealta.com.br");
        email.setSubject("[TESTE] " + email.subject());
        email.setBody("Destinatário original: " + originalTo + "\n" + email.body());
        return ProcessingResult.passed(email);
    }
}
