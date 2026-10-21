package com.pousadamarealta.email;

public class ArchiveCopyRule extends EmailRule {
    private final boolean testMode;
    private final boolean enabled;
    private final EmailRuleChain chain;

    public ArchiveCopyRule(boolean testMode, boolean enabled, EmailRuleChain chain) {
        this.testMode = testMode;
        this.enabled = enabled;
        this.chain = chain;
    }

    @Override
    protected boolean shouldProcess(EmailMessage message) {
        return enabled && !testMode && !message.isBlocked();
    }

    @Override
    protected void doProcess(EmailMessage message) {
        EmailMessage copy = new EmailMessage(
            "arquivo@pousadamarealta.com.br",
            "[Cópia] " + message.getSubject(),
            message.getBody()
        );
        chain.addCopy(copy);
    }
}
