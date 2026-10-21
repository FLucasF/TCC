package com.pousadamarealta.email;

public class TestModeRule extends EmailRule {
    private final boolean testMode;

    public TestModeRule(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    protected boolean shouldProcess(EmailMessage message) {
        return testMode;
    }

    @Override
    protected void doProcess(EmailMessage message) {
        String originalTo = message.getOriginalTo();
        message.setTo("testes@pousadamarealta.com.br");
        message.setSubject("[TESTE] " + message.getSubject());
        message.setBody("Destinatário original: " + originalTo + "\n\n" + message.getBody());
    }
}
