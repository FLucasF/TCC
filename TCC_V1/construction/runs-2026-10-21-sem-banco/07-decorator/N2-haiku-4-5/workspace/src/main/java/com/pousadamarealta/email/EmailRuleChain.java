package com.pousadamarealta.email;

import java.util.ArrayList;
import java.util.List;

public class EmailRuleChain {
    private EmailRule firstRule;
    private final List<EmailMessage> copies = new ArrayList<>();

    public EmailRuleChain(boolean testMode, boolean archiveEnabled) {
        this.firstRule = new ValidationRule();
        EmailRule testRule = new TestModeRule(testMode);
        ArchiveCopyRule archiveRule = new ArchiveCopyRule(testMode, archiveEnabled, this);

        firstRule.setNext(testRule);
        testRule.setNext(archiveRule);
    }

    public void process(EmailMessage message) {
        copies.clear();
        firstRule.process(message);
    }

    public void addCopy(EmailMessage copy) {
        copies.add(copy);
    }

    public List<EmailMessage> getCopies() {
        return new ArrayList<>(copies);
    }
}
